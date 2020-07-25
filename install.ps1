#Requires -Version 5

$erroractionpreference = 'stop' # quit if anything goes wrong

if (($PSVersionTable.PSVersion.Major) -lt 5) {
    Write-Output "PowerShell 5 or later is required to run this installer."
    Write-Output "Upgrade PowerShell: https://docs.microsoft.com/en-us/powershell/scripting/setup/installing-windows-powershell"
    break
}

$allowedExecutionPolicy = @('Unrestricted', 'RemoteSigned', 'ByPass')
if ((Get-ExecutionPolicy).ToString() -notin $allowedExecutionPolicy) {
    Write-Output "PowerShell requires an execution policy in [$($allowedExecutionPolicy -join ", ")] for this installer."
    Write-Output "For example, to set the execution policy to 'RemoteSigned' please run :"
    Write-Output "'Set-ExecutionPolicy RemoteSigned -scope CurrentUser'"
    break
}

if ([System.Enum]::GetNames([System.Net.SecurityProtocolType]) -notcontains 'Tls12') {
    Write-Output "This installer requires at least .NET Framework 4.5"
    Write-Output "Please download and install it first:"
    Write-Output "https://www.microsoft.com/net/download"
    break
}

$releast_version_url = "https://raw.githubusercontent.com/borkdude/deps.clj/master/resources/DEPS_CLJ_RELEASED_VERSION"
$latest_release = (Invoke-WebRequest -Uri $releast_version_url -UseBasicParsing)
$latest_release = $latest_release.toString().trim()

$tmp_dir = $env:TEMP
$download_url = "https://github.com/borkdude/deps.clj/releases/download/v$latest_release/deps.clj-$latest_release-windows-amd64.zip"
$tmp_zip_file = "$tmp_dir\deps.clj.zip"

Write-Output "Downloading..."
(New-Object System.Net.WebClient).DownloadFile($download_url,"$tmp_zip_file")
Write-Output 'Extracting...'
Expand-Archive -LiteralPath "$tmp_zip_file" -DestinationPath "$tmp_dir" -Force

function Make-Dir($dir) {
    if(!(test-path $dir)) {
        mkdir $dir > $null }; 
}

$deps_clj_dir = "$env:USERPROFILE\deps.clj"
$tmp_exe_file = "$tmp_dir\deps.exe"
$installed_exe_file = "$deps_clj_dir\deps.exe"
$installed_exe_file_backup = "$deps_clj_dir\deps.exe.bak"

Make-Dir($deps_clj_dir)
Write-Output "Installing deps.exe to $deps_clj_dir..."
if (Test-Path -Path "$installed_exe_file") {
  Move-Item -Path "$installed_exe_file" -Destination "$installed_exe_file_backup" -Force
}
Move-Item -Path "$tmp_exe_file" -Destination "$installed_exe_file" -Force

function env($name,$global,$val='__get') {
    $target = 'User'; if($global) {$target = 'Machine'}
    if($val -eq '__get') { [environment]::getEnvironmentVariable($name,$target) }
    else { [environment]::setEnvironmentVariable($name,$val,$target) }
}
function fullpath($path) { # should be ~ rooted
    $executionContext.sessionState.path.getUnresolvedProviderPathFromPSPath($path)
}

function friendly_path($path) {
    $h = (Get-PsProvider 'FileSystem').home; if(!$h.endswith('\')) { $h += '\' }
    if($h -eq '\') { return $path }
    return "$path" -replace ([regex]::escape($h)), "~\"
}

function ensure_in_path($dir, $global) {
    $path = env 'PATH' $global 
    $dir = fullpath $dir
    if($path -notmatch [regex]::escape($dir)) {
        write-output "Adding $(friendly_path $dir) to $(if($global){'global'}else{'your'}) path."
        env 'PATH' $global "$dir;$path" # for future sessions...
        $env:PATH = "$dir;$env:PATH" # for this session
    }
}

ensure_in_path("$deps_clj_dir")

Write-Output "Cleaning up..."
Remove-Item "$tmp_zip_file"

Write-Output "Succesfully installed deps.exe."
Write-Output "Restart cmd.exe for changes to the path to take effect."
