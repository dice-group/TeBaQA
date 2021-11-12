@ECHO OFF
TITLE TeBaQA Installation

:: Notes:
:: 1. Run this script from the root folder

:: Install and run ES
:: TODO check if ES already exists

ECHO ==============================================
ECHO Downloading elasticsearch-6.6.1 ...
ECHO ==============================================
curl.exe -o elasticsearch-6.6.1.zip https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.6.1.zip
tar -xf elasticsearch-6.6.1.zip
:: cd elasticsearch-6.6.1/bin
:: nohup ./elasticsearch &
ECHO ==============================================
ECHO Success.
ECHO ==============================================

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

PAUSE