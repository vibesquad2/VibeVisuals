param([Parameter(Mandatory=$true)][string]$Answer)
$ErrorActionPreference = "Stop"
$dir = "C:\Users\Lenovo\Desktop\VibeVisuals\bcrypt_crack"
$f = Get-Content "$dir\captcha_fields.json" | ConvertFrom-Json
$cookies = Get-Content "$dir\cookies.json" | ConvertFrom-Json
$sess = New-Object Microsoft.PowerShell.Commands.WebRequestSession
foreach ($c in $cookies) {
    $sess.Cookies.Add((New-Object System.Net.Cookie($c.Name, $c.Value, "/", "hashes.com")))
}
$hash = '$2a$10$t2PEgq2vg4byrg7L/cJTx.nkPo7Ry3ICHAumLbodu6FG/AeZiIdIq'
$body = @{
    csrf_token = $f.csrf
    hashes = $hash
    u = $f.u
    captchaIdentifier = $f.cid
    captcha = $Answer
    submitted = "true"
}
$r = Invoke-WebRequest -Uri "https://hashes.com/en/decrypt/hash" -Method POST -Body $body -UseBasicParsing -WebSession $sess -TimeoutSec 60
Set-Content "$dir\result.html" $r.Content
$text = ($r.Content -replace '(?s)<script.*?</script>',' ' -replace '(?s)<style.*?</style>',' ' -replace '<[^>]+>',' ' -replace '&nbsp;',' ' -replace '\s+',' ')
$idx = $text.IndexOf("Hashes")
if ($idx -ge 0) { $text.Substring($idx, [Math]::Min(3000, $text.Length - $idx)) } else { $text.Substring(0, [Math]::Min(3000, $text.Length)) }
