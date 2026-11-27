#requires -Version 7.0

# The same source defines copy verification and the optional process-local hash reader.
function Get-AtfCheckedFileIoDefinition {
@'
using System;
using System.IO;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Runtime.ExceptionServices;
using System.Threading.Tasks;
using System.Security.Cryptography;
namespace Atf {
    public static class CheckedFileIo {
        public static string Sha256(string path) {
            using (var stream = File.OpenRead(path))
                return Convert.ToHexString(SHA256.HashData(stream)).ToLowerInvariant();
        }
        public static void AssertDirect(string path) {
            AssertDirect(path, null);
        }
        private static void AssertDirect(string path, HashSet<string> checkedPaths) {
            for (string current = path; current != null; current = Directory.GetParent(current)?.FullName) {
                if (checkedPaths != null && !checkedPaths.Add(current)) break;
                if ((File.Exists(current) || Directory.Exists(current)) &&
                    (File.GetAttributes(current) & FileAttributes.ReparsePoint) != 0)
                    throw new IOException("SOURCE-STAGE-INDIRECT: source and staging paths must not traverse links.");
            }
        }
        public static void Copy(string[] sources, string[] targets, string[] hashes, string[] labels) {
            if (sources.Length != targets.Length || sources.Length != hashes.Length || sources.Length != labels.Length)
                throw new ArgumentException("SOURCE-STAGE-POPULATION");
            // The entire admission pass precedes every directory creation and copy.
            var admitted = new HashSet<string>(StringComparer.Ordinal);
            for (int i = 0; i < sources.Length; i++) { AssertDirect(sources[i], admitted); AssertDirect(targets[i], admitted); }
            var targetsSeen = new HashSet<string>(RuntimeInformation.IsOSPlatform(OSPlatform.Windows)
                ? StringComparer.OrdinalIgnoreCase : StringComparer.Ordinal);
            foreach (string target in targets)
                if (!targetsSeen.Add(target)) throw new IOException("SOURCE-STAGE-DUPLICATE");
            try { Parallel.For(0, sources.Length, new ParallelOptions { MaxDegreeOfParallelism = 4 }, i => {
                try {
                    // Recheck paths at use; no ancestor result is cached between files.
                    var atUse = new HashSet<string>(StringComparer.Ordinal);
                    AssertDirect(sources[i], atUse); AssertDirect(targets[i], atUse);
                    Directory.CreateDirectory(Path.GetDirectoryName(targets[i]));
                    if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows)) {
                        // CopyFile's per-file overhead dominates small source trees on Windows.
                        using (var source = File.OpenRead(sources[i]))
                        using (var target = File.Create(targets[i])) source.CopyTo(target);
                        File.SetLastWriteTimeUtc(targets[i], File.GetLastWriteTimeUtc(sources[i]));
                        File.SetAttributes(targets[i], File.GetAttributes(sources[i]));
                    } else {
                        // Preserve the platform's original mode/attribute copy semantics.
                        File.Copy(sources[i], targets[i], true);
                    }
                    if (!String.Equals(Sha256(targets[i]), hashes[i], StringComparison.OrdinalIgnoreCase))
                        throw new IOException("SOURCE-STAGE-CHANGED: " + labels[i]);
                } catch (FileNotFoundException error) {
                    if (!File.Exists(sources[i])) throw new IOException("SOURCE-STAGE-CHANGED: " + labels[i], error);
                    throw;
                } catch (DirectoryNotFoundException error) {
                    if (!File.Exists(sources[i])) throw new IOException("SOURCE-STAGE-CHANGED: " + labels[i], error);
                    throw;
                }
            }); } catch (AggregateException error) {
                ExceptionDispatchInfo.Capture(error.Flatten().InnerExceptions[0]).Throw();
                throw;
            }
        }
    }
}
'@
}

function Get-AtfCheckedFileIoSourceHash {
    $bytes = [Text.Encoding]::UTF8.GetBytes((Get-AtfCheckedFileIoDefinition))
    [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes)).ToLowerInvariant()
}

# Compile only for copying. Hash-only callers never acquire a compiler startup dependency.
function Initialize-AtfCheckedFileCopy {
    if (-not ('Atf.CheckedFileIo' -as [type])) {
        Add-Type -TypeDefinition (Get-AtfCheckedFileIoDefinition)
        $reader = [Atf.CheckedFileIo].GetMethod('Sha256').CreateDelegate([Func[string,string]])
        [AppDomain]::CurrentDomain.SetData('ATF.CheckedFileIo.HashReader.1', @{
            definitionSha256=(Get-AtfCheckedFileIoSourceHash); reader=$reader
        })
    }
}

function Get-AtfPreparedFileHasher {
    $state = [AppDomain]::CurrentDomain.GetData('ATF.CheckedFileIo.HashReader.1')
    $type = 'Atf.CheckedFileIo' -as [type]
    if ($state -isnot [Collections.IDictionary] -or !$state.Contains('reader') -or
            !$state.Contains('definitionSha256') -or $state.reader -isnot [Func[string,string]] -or !$type) { return }
    if ($state.definitionSha256 -cne (Get-AtfCheckedFileIoSourceHash) -or
            $state.reader.Method -ne $type.GetMethod('Sha256', [type[]]@([string])) -or
            $state.reader.GetInvocationList().Length -ne 1 -or $null -ne $state.reader.Target) { return }
    $state.reader
}

# Fresh content reads only: file times and sizes never authorize reuse.
function Get-AtfFileSha256([string]$Path) {
    $stream = [IO.File]::OpenRead($Path)
    try { [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($stream)).ToLowerInvariant() }
    finally { $stream.Dispose() }
}
