# UML Class Diagram Visualizer

A minimal JavaFX application that allows you to input files and output a class diagram from PlantUML.

Below is a step-by-step process to get this to run on your computer (\****Windows 11 ONLY**\**)

## Step 1: Check if Maven is installed
Run this in your Powershell/Terminal:
```bash
mvn -v
```
If you get an error, continue to step 2.

If not, skip to step 6.


## Step 2: Download Maven
Go to https://maven.apache.org/download.cgi or click below...

Download the binary zip archive: https://dlcdn.apache.org/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.zip

## Step 3: Installation
Unzip/extract to somewhere permanent.

Preferably within Program Files or your drive
> C:\apache-maven-3.9.11


## Step 4: Set Maven Environment Variables
> **This will require you to edit internal Windows systems**

- Press **Win + R**, type `sysdm.cpl`, hit Enter.
- Go to **Advanced** → **Environment Variables**.
- Under **System variables**, find `Path`, click Edit or double-click
- Now, click New and Add (Make sure your file path includes the actual **bin folder**)...
  - > C:\apache-maven-3.9.11\bin
- Also add a new variable under **System variables**:
  - Name: MAVEN_HOME
  - Value: C:\apache-maven-3.9.11

## Step 5: Set Java Environment Variables
> **This will require you to edit internal Windows systems**

Continuing from Step 4.

- Find where your Java JDK is installed: `C:\Program Files\Java\jdk-17` or `C:\Users\Fakeuser\.jdks\corretto-24.0.2`
- Add a new system variable:
  - Variable name: JAVA_HOME
  - Variable value: C:\Program Files\Java\jdk-17
- Edit `Path` again and add: 
  - >%JAVA_HOME%\bin




## Step 5: Verify Installation
Reload your Powershell/Command Line
OR
restart Intellij

Now once again run the following command in Powershell
```bash
mvn -v
```
- You should see Maven version info and Java version info.

## Step 6: Run the Project
1. Navigate to your project folder (Unless you are already in Intellij, then run this in your terminal)
2. Now run:
```bash
mvn clean javafx:run 
```
