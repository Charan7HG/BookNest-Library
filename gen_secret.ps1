$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes) | Out-File -FilePath .\.jwt-secret-temp.txt -Encoding ascii
