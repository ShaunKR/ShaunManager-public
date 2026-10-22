$ErrorActionPreference = 'Stop'
$base = $PSScriptRoot
$inputFile = Join-Path $base 'config\amaranth-login.json'
$outputFile = Join-Path $base 'config\amaranth-login.enc'
$jar = Join-Path $base 'shaun-manager.jar'
$java = Join-Path $base 'jre\bin\java.exe'
if (!(Test-Path -LiteralPath $java)) { $java = (Get-Command java -ErrorAction Stop).Source }
if (!(Test-Path -LiteralPath $inputFile)) { throw 'Missing config/amaranth-login.json' }
if (!(Test-Path -LiteralPath $jar)) { throw 'Missing shaun-manager.jar' }
if (Test-Path -LiteralPath $outputFile) { throw 'Encrypted output already exists; no files were changed.' }
$key = [Environment]::GetEnvironmentVariable('SHAUN_CONFIG_KEY','User')
if ([string]::IsNullOrWhiteSpace($key)) {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $key = [Convert]::ToBase64String($bytes)
    [Array]::Clear($bytes,0,$bytes.Length)
    [Environment]::SetEnvironmentVariable('SHAUN_CONFIG_KEY',$key,'User')
}
$env:SHAUN_CONFIG_KEY = $key
& $java '-Dloader.main=knr.shaunmanager.amaranth.LoginConfigCrypto' -cp $jar org.springframework.boot.loader.PropertiesLauncher $inputFile $outputFile
if ($LASTEXITCODE -ne 0) { throw 'Encryption failed. Original configuration has been retained.' }
# The Java utility verifies decryption before reporting success.
Remove-Item -LiteralPath $inputFile
Write-Host 'Done. Encrypted file created; original JSON removed. Key saved for this Windows user.'
Write-Host 'Start the server as this same user from a newly opened terminal.'
