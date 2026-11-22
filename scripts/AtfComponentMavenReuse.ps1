#requires -Version 7.0

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
                                      [string[]]$Modules, [string]$TempRoot) {
    if (!$Previous) { return }
    $lease = $null
    $reference = $null
    try {
        $pointer = Get-Content -LiteralPath (Join-Path $Previous 'component-workspace.json') -Raw | ConvertFrom-Json
        if ($pointer.schemaVersion -ne 1) { return }
        $owner = Get-AtfCompilerWorkspacePath $pointer.path $TempRoot
        $referenceFile = Join-Path $owner 'component-maven-reference.json'
        if (Test-Path -LiteralPath $referenceFile -PathType Leaf) {
            $reference = Get-Content -LiteralPath $referenceFile -Raw | ConvertFrom-Json
            if ($reference.schemaVersion -ne 1 -or $reference.identity -cne $Identity) { return }
            $owner = Get-AtfCompilerWorkspacePath $reference.buildWorkspace $TempRoot
        }
        if ($owner -ieq $Work) { return }
        $lease = Open-AtfQualificationWorkspace $owner
        $receiptFile = Join-Path $owner 'component-maven.json'
        $receipt = Get-Content -LiteralPath $receiptFile -Raw | ConvertFrom-Json
        if ($receipt.schemaVersion -ne 2) { return }
        if ($reference -and (Get-FileHash -LiteralPath $receiptFile).Hash.ToLowerInvariant() -cne $reference.receiptSha256) { return }
        $prepared = Read-AtfPreparedComponentMaven $owner $Identity $Modules
        if (!$prepared -or !$prepared.classpathReady) { return }
        $prepared | Add-Member -NotePropertyName lease -NotePropertyValue $lease
        $prepared | Add-Member -NotePropertyName buildWorkspace -NotePropertyValue $owner
        $lease = $null
        $prepared
    } catch {
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
