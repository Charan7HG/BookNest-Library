powershell -c  $bytes = New-Object -TypeName 'System.Security.Cryptography.RNGCryptoServiceProvider'; $bytes.GetBytes(32) | % { [System.Convert]::ToBase64String($_) } > .jwt-secret-temp.txt
