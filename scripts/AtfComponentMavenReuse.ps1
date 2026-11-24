#requires -Version 7.0
. (Join-Path $PSScriptRoot 'AtfComponentWorkspace.ps1')

function Get-AtfCompilerWorkspacePath([string]$Path, [string]$TempRoot) {
    $full = [IO.Path]::GetFullPath($Path)
    if ((Split-Path -Parent $full) -ine [IO.Path]::GetFullPath($TempRoot) -or
            (Split-Path -Leaf $full) -cnotmatch '^whist-component-suites-[a-f0-9]{32}$') {
        throw 'COMPONENT_COMPILER_WORKSPACE_PERIMETER'
    }
    $entry = Get-Item -LiteralPath $full -Force
    Assert-AtfCompilerDirectPath $full
    if (!$entry.PSIsContainer -or ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw 'COMPONENT_COMPILER_WORKSPACE_INDIRECT'
    }
    $full
}

function Read-AtfBorrowedComponentMaven([string]$Previous, [string]$Work, [string]$Identity,
                                      [string[]]$Modules, [string]$TempRoot, [hashtable]$Decision = @{}) {
    if (!$Previous) { return }
    $lease = $null
    $reference = $null
    try {
        $pointer = (Read-AtfComponentWorkspace $Previous).pointer
        $owner = Get-AtfCompilerWorkspacePath $pointer.path $TempRoot
        $referenceFile = Join-Path $owner 'component-maven-reference.json'
        if (Test-Path -LiteralPath $referenceFile -PathType Leaf) {
            $reference = Get-Content -LiteralPath $referenceFile -Raw | ConvertFrom-Json
            if ($reference.schemaVersion -ne 1 -or $reference.identity -cne $Identity) { throw 'COMPONENT_COMPILER_REFERENCE_IDENTITY' }
            $owner = Get-AtfCompilerWorkspacePath $reference.buildWorkspace $TempRoot
        }
        if ($owner -ieq $Work) { throw 'COMPONENT_COMPILER_SELF_REFERENCE' }
        $lease = Open-AtfQualificationWorkspace $owner
        $receiptFile = Join-Path $owner 'component-maven.json'
        $receipt = Get-Content -LiteralPath $receiptFile -Raw | ConvertFrom-Json
        if ($receipt.schemaVersion -ne 2) { throw 'COMPONENT_COMPILER_RECEIPT_SCHEMA' }
        if ($receipt.identity -cne $Identity) { throw 'COMPONENT_COMPILER_IDENTITY_CHANGED' }
        if ($reference -and (Get-FileHash -LiteralPath $receiptFile).Hash.ToLowerInvariant() -cne $reference.receiptSha256) { throw 'COMPONENT_COMPILER_RECEIPT_SEAL' }
        $prepared = Read-AtfPreparedComponentMaven $owner $Identity $Modules
        if (!$prepared) { throw 'COMPONENT_COMPILER_OUTPUTS_UNQUALIFIED' }
        if (!$prepared.classpathReady) { throw 'COMPONENT_COMPILER_CLASSPATH_INCOMPLETE' }
        $prepared | Add-Member -NotePropertyName lease -NotePropertyValue $lease
        $prepared | Add-Member -NotePropertyName buildWorkspace -NotePropertyValue $owner
        $lease = $null
        $Decision.status = 'REUSED'; $Decision.reason = 'Verified compiler identity, original context, output bytes and leased classpath.'
        $prepared
    } catch {
        $Decision.status = 'REFUSED'; $Decision.reason = $_.Exception.Message
        Write-Verbose "Compiler reuse rejected: $($_.Exception.Message)"
    } finally { if ($lease) { $lease.Dispose() } }
}

function Write-AtfComponentMavenReference([string]$Work, $Prepared, [string]$Identity) {
    $receiptFile = Join-Path $Prepared.buildWorkspace 'component-maven.json'
    Write-AtfComponentJson (Join-Path $Work 'component-maven-reference.json') ([ordered]@{
        schemaVersion=1;identity=$Identity;buildWorkspace=$Prepared.buildWorkspace
        receiptSha256=(Get-FileHash -LiteralPath $receiptFile).Hash.ToLowerInvariant()
    })
}
