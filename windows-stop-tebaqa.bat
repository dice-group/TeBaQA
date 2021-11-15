@ECHO OFF
TITLE Stopping TeBaQA ...

cd elasticsearch-6.6.1/bin
CALL elasticsearch-service.bat stop
cd ..
cd ..

wmic Path win32_process Where "CommandLine Like '%%tebaqa-controller-1.0.jar%%'" Call Terminate
wmic Path win32_process Where "CommandLine Like '%%query-ranking-1.0.jar%%'" Call Terminate
wmic Path win32_process Where "CommandLine Like '%%entity-linking-1.0.jar%%'" Call Terminate
wmic Path win32_process Where "CommandLine Like '%%template-classification-1.0.jar%%'" Call Terminate
wmic Path win32_process Where "CommandLine Like '%%nlp-1.0.jar%%'" Call Terminate

wmic Path win32_process Where "CommandLine Like '%%fuseki-server.jar%%'" Call Terminate

PAUSE