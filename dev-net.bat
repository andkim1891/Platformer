@echo off
Setlocal enabledelayedexpansion

:: ============================================================================
:: Firewall Management Tool (with Duplicate Prevention)
::
:: 1. Displays all current firewall rules.
:: 2. Checks for existing rules to avoid duplicates.
:: 3. Minecraft: Java (25565 TCP/UDP), Bedrock (19132/19133 UDP).
:: 4. Flask production server (main.py): TCP 5000 on 0.0.0.0.
:: ============================================================================

:: Check for Administrative Privileges
Net session >nul 2>&1
If %errorLevel% neq 0 (
    Echo [ERROR] This script requires Administrator privileges.
    Pause
    Exit /b 1
)



Echo [STEP 1] DELETING FIREWALL RULES...
Echo ----------------------------------------------------------------------------
Echo Deleting all firewall rules named 'java.exe'...
Netsh advfirewall firewall delete rule name="java.exe"
Echo Deleting all firewall rules named 'javaw.exe'...
Netsh advfirewall firewall delete rule name="javaw.exe"
Echo Deleting all firewall rules named 'OpenJDK Platform binary'...
Netsh advfirewall firewall delete rule name="OpenJDK Platform binary"
Echo Deleting all firewall rules named 'Java(TM) Platform SE binary'...
Netsh advfirewall firewall delete rule name="Java(TM) Platform SE binary"
Echo Deleting all firewall rules named 'Minecraft_Server_Java'...
netsh advfirewall firewall delete rule name="Minecraft_Server_Java"
Echo Deleting all firewall rules named 'Minecraft_Server_Bedrock'...
netsh advfirewall firewall delete rule name="Minecraft_Server_Bedrock"


Echo Deleting all firewall rules named 'python.exe'...
Netsh advfirewall firewall delete rule name="python.exe"
Echo Deleting all firewall rules named 'Python'...
Netsh advfirewall firewall delete rule name="Python"
Echo Deleting all firewall rules named 'Flask_Server'...
Netsh advfirewall firewall delete rule name="Flask_Server"

Echo.
Echo Cleanup complete. Windows will prompt you again the next time Java needs access.
Echo ----------------------------------------------------------------------------
Pause


:CHECK_JAVA
:: Check if the rule name already exists
Netsh advfirewall firewall show rule name="Minecraft_Server_Java" >nul 2>&1
If %errorLevel% equ 0 (
    Echo [INFO] Rule "Minecraft_Server_Java" already exists. Skipping prompt.
) else (
    Echo [STEP 2] MINECRAFT JAVA EDITION
    Set /p "choice_java=Add Minecraft Java Rule (TCP 25565/UDP 25565)? [y/n]: "
    If /I "!choice_java!"=="y" (
        netsh advfirewall firewall add rule name="Minecraft_Server_Java" dir=in action=allow protocol=TCP localport=25565 profile=any
        netsh advfirewall firewall add rule name="Minecraft_Server_Java" dir=in action=allow protocol=UDP localport=25565 profile=any
        Echo Rule "Minecraft_Server_Java" added successfully.
    )
)
Echo.

:CHECK_BEDROCK
:: Check if the rule name already exists
Netsh advfirewall firewall show rule name="Minecraft_Server_Bedrock" >nul 2>&1
If %errorLevel% equ 0 (
    Echo [INFO] Rule "Minecraft_Server_Bedrock" already exists. Skipping prompt.
) else (
    Echo [STEP 3] MINECRAFT BEDROCK EDITION
    Set /p "choice_bedrock=Add Minecraft Bedrock Rule (UDP 19132/19133)? [y/n]: "
    If /I "!choice_bedrock!"=="y" (
        netsh advfirewall firewall add rule name="Minecraft_Server_Bedrock" dir=in action=allow protocol=UDP localport=19132 profile=any
        netsh advfirewall firewall add rule name="Minecraft_Server_Bedrock" dir=in action=allow protocol=UDP localport=19133 profile=any
        Echo Rule "Minecraft_Server_Bedrock" added successfully.
    )
)




:CHECK_FLASK
:: Flask production server (main.py: host 0.0.0.0, port 5000)
Netsh advfirewall firewall show rule name="Flask_Server" >nul 2>&1
If %errorLevel% equ 0 (
    Echo [INFO] Rule "Flask_Server" already exists. Skipping prompt.
) else (
    Echo [STEP 4] FLASK SERVER (main.py, port 5000)
    Set /p "choice_flask=Add Flask server rule (TCP 5000)? [y/n]: "
    If /I "!choice_flask!"=="y" (
        netsh advfirewall firewall add rule name="Flask_Server" dir=in action=allow protocol=TCP localport=5000 profile=any
        Echo Rule "Flask_Server" added successfully.
    )
)
Echo.

:DISPLAY_RULES
Echo [STEP 5] DISPLAYING ALL CURRENT FIREWALL RULES...
Echo ----------------------------------------------------------------------------
netsh advfirewall firewall show rule name=all > "%~dp0fw_rules_temp.txt"
Echo ----------------------------------------------------------------------------
Echo.


Echo.
Echo ============================================================================
Echo Process finished.
Echo ============================================================================
Pause
Exit /b 0


