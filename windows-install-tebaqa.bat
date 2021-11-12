@ECHO OFF
TITLE TeBaQA Installation

:: Note: Run this script from the root folder

:: Install ES
IF EXIST elasticsearch-6.6.1\ (
    ECHO elasticsearch-6.6.1 already installed
) ELSE (
    ECHO ==============================================
    ECHO Downloading elasticsearch-6.6.1 ...
    ECHO ==============================================
    CALL curl.exe -o elasticsearch-6.6.1.zip https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.6.1.zip
    CALL tar -xf elasticsearch-6.6.1.zip
    :: Remove ES service if it already exists and then install new
    cd elasticsearch-6.6.1/bin
    CALL elasticsearch-service.bat remove
    CALL elasticsearch-service.bat install
    cd ..
    cd ..
    ECHO ==============================================
    ECHO Success.
    ECHO ==============================================
)

:: Install and build TeBaQA
IF EXIST TeBaQA-speaker-integration\ (
	setlocal
	:PROMPT
    choice /C YN /N /M "TeBaQA is already installed in this directory. Do you want to overwrite it [Y/N]?"
	IF NOT ERRORLEVEL 1 GOTO PROMPT
	IF ERRORLEVEL 2 GOTO :EOF
    endlocal
    GOTO :DOINSTALL
)
ELSE (
    :DOINSTALL
    ECHO ==============================================
    ECHO Downloading TeBaQA
    ECHO ==============================================
    :: Get codebase
    curl.exe -L -o speaker-integration.zip https://github.com/dice-group/TeBaQA/archive/speaker-integration.zip
    tar -xf speaker-integration.zip
    ECHO ==============================================
    ECHO Success.
    ECHO ==============================================

    ECHO ==============================================
    ECHO Building TeBaQA
    ECHO ==============================================
    :: Build modules
    cd TeBaQA-speaker-integration
    CALL windows-build-tebaqa.bat
)

PAUSE