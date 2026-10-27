$ErrorActionPreference = "Stop"
$dir = "C:\Users\Lenovo\Desktop\VibeVisuals\bcrypt_crack"
$sess = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$r = Invoke-WebRequest -Uri "https://hashes.com/en/decrypt/hash" -UseBasicParsing -WebSession $sess -TimeoutSec 30
$html = $r.Content
$u = [regex]::Match($html, 'name="u" value="([^"]+)"').Groups[1].Value
$cid = [regex]::Match($html, 'name="captchaIdentifier" value="([^"]+)"').Groups[1].Value
$csrf = [regex]::Match($html, 'name="csrf_token" value="([^"]+)"').Groups[1].Value
$img = [regex]::Match($html, 'src="data:image/jpeg;base64,([^"]+)"').Groups[1].Value
[IO.File]::WriteAllBytes("$dir\captcha.jpg", [Convert]::FromBase64String($img))
@{ u=$u; cid=$cid; csrf=$csrf } | ConvertTo-Json | Set-Content "$dir\captcha_fields.json"
$sess.Cookies.GetCookies("https://hashes.com") | Select-Object Name,Value | ConvertTo-Json | Set-Content "$dir\cookies.json"
Write-Output "u=$u"
Write-Output "cid=$cid"
Write-Output "csrf=$csrf"
