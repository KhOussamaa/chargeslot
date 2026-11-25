param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
function New-DemoAccount([string]$Username, [string]$Password) {
    $basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${Username}:$Password"))
    $headers = @{ Authorization = "Basic $basic" }
    $token = Invoke-RestMethod -Uri "$BaseUrl/api/csrf" -Headers $headers -SessionVariable chargeSession
    $headers[$token.headerName] = $token.token
    return [pscustomobject]@{ Headers=$headers; Session=$chargeSession; Username=$Username }
}
function Invoke-DemoApi($Account, [string]$Method, [string]$Path, $Payload = $null) {
    $parameters = @{ Uri="$BaseUrl/api$Path"; Method=$Method; Headers=$Account.Headers; WebSession=$Account.Session }
    if ($null -ne $Payload) { $parameters.ContentType='application/json'; $parameters.Body=($Payload | ConvertTo-Json -Depth 8) }
    return Invoke-RestMethod @parameters
}
$aliceAccount = New-DemoAccount 'alice' 'AliceDemo21!'
$bobAccount = New-DemoAccount 'bob' 'BobDemo21!'
$network = Invoke-DemoApi $aliceAccount 'GET' '/stations'
$campus = $network | Where-Object name -eq 'Campus' | Select-Object -First 1
if ($null -eq $campus) { throw 'Station Campus absente. Utilisez une base de demonstration vierge.' }
$ccs = @($campus.connectors | Where-Object { $_.type -eq 'CCS' -and $_.active })
if ($ccs.Count -lt 2) { throw 'Deux connecteurs CCS actifs sont necessaires a Campus.' }
$aliceVehicles = Invoke-DemoApi $aliceAccount 'GET' '/vehicles'
$bobVehicles = Invoke-DemoApi $bobAccount 'GET' '/vehicles'
$aliceVehicle = $aliceVehicles | Where-Object { $_.connectorType -eq 'CCS' } | Select-Object -First 1
$bobVehicle = $bobVehicles | Where-Object { $_.connectorType -eq 'CCS' } | Select-Object -First 1
if ($null -eq $aliceVehicle -or $null -eq $bobVehicle) { throw 'Vehicules de demonstration absents.' }
$startDate = [DateTime]::UtcNow.Date.AddDays(1).AddHours(14)
$start = $startDate.ToString('yyyy-MM-ddTHH:mm:ssZ')
$end = $startDate.AddHours(1).ToString('yyyy-MM-ddTHH:mm:ssZ')
$created = [Collections.Generic.List[object]]::new()
try {
    $first = Invoke-DemoApi $aliceAccount 'POST' '/reservations' @{driverId=17;vehicleId=$aliceVehicle.id;connectorId=$ccs[0].id;startAt=$start;endAt=$end;requestedPowerKw=50}
    $created.Add([pscustomobject]@{Account=$aliceAccount;Id=$first.id})
    Write-Host "OK - Alice : reservation #$($first.id), 50 kW, $($first.status)"
    $rejected = $false
    try {
        $unexpected = Invoke-DemoApi $bobAccount 'POST' '/reservations' @{driverId=18;vehicleId=$bobVehicle.id;connectorId=$ccs[1].id;startAt=$start;endAt=$end;requestedPowerKw=40}
        $created.Add([pscustomobject]@{Account=$bobAccount;Id=$unexpected.id})
    } catch {
        if ([int]$_.Exception.Response.StatusCode -ne 409) { throw }
        $rejected = $true
        Write-Host 'OK - Bob : demande 40 kW refusee avec HTTP 409 (50 + 40 > 80).'
        Write-Host $_.ErrorDetails.Message
    }
    if (-not $rejected) { throw 'La demande excedant la capacite a ete acceptee a tort.' }
    $second = Invoke-DemoApi $bobAccount 'POST' '/reservations' @{driverId=18;vehicleId=$bobVehicle.id;connectorId=$ccs[1].id;startAt=$start;endAt=$end;requestedPowerKw=30}
    $created.Add([pscustomobject]@{Account=$bobAccount;Id=$second.id})
    Write-Host "OK - Bob : reservation #$($second.id), 30 kW (50 + 30 = 80)."
    $notes = Invoke-DemoApi $aliceAccount 'GET' '/notifications'
    Write-Host "OK - Alice voit $(@($notes).Count) notification(s) locale(s)."
} finally {
    foreach ($item in $created) {
        try {
            $cancelled = Invoke-DemoApi $item.Account 'POST' "/reservations/$($item.Id)/cancel"
            Write-Host "Nettoyage - reservation #$($item.Id) : $($cancelled.status)"
        } catch { Write-Warning "Annulation a verifier pour #$($item.Id) : $($_.Exception.Message)" }
    }
}
