Param (
    [Parameter(HelpMessage = "The Build Number to set in the Version attribute of the files")]
    [Parameter(ParameterSetName = 'bNumber')]
    [Alias("b")]
    [int]$buildNum = -1,

    [Alias("t")]
    [string]$type = ""
)

if (![string]::IsNullOrEmpty($type))
{
    $type = "-" + $type
}
$buildGradle = [IO.File]::ReadAllText(".\build.gradle");
#$pattern = '${1}' + $buildNum + '${2}' + "'"
$pattern = '${1}' + $buildNum + $type + "'"
$buildGradle = $buildGradle -replace "(version = '[0-9][.][0-9][.][0-9][.])[0-9]*([^']*)'", $pattern
[IO.File]::WriteAllText(".\build.gradle", $buildGradle)
$pattern = '${1}' + $buildNum + $type
$buildGradle.replace("`n", ", ").replace("`r", ", ") -replace ".*version = '([0-9][.][0-9][.][0-9][.])[0-9]*([^']*).*", $pattern
