# PowerShell 프로필 설정
# 한글 인코딩 설정
chcp 65001 | Out-Null

# 환경 변수 설정
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Duser.language=ko -Duser.country=KR"

# 콘솔 출력 인코딩 설정
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "PowerShell 프로필이 로드되었습니다. (UTF-8 인코딩 설정됨)" -ForegroundColor Green 