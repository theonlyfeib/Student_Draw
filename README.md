# Student Draw

A small Java Swing desktop app for managing multiple classes and events,
randomly drawing students, and tracking who has already been drawn. The class
roster shows event progress and marks students who have already been selected.

## Launch

Java 8 or later is required to run from source. From the project folder, run:

```text
javac Main.java
java Main
```

## Windows installer

Install JDK 27 or later and WiX Toolset 3.14.1, then run this from PowerShell:

```powershell
.\build-installer.ps1
```

The self-contained installer is created in `dist`. It includes a Java runtime,
creates a Start Menu entry and desktop shortcut, and installs for the current
Windows user. Use `.\build-installer.ps1 -Type app-image` to build a runnable
application folder without WiX.

The app stores its data in `%APPDATA%\Student Draw\student-draw-data.txt`.
If the previous `estrazione-studenti-data.txt` file is present in the launch
folder, its data is copied to the new location on first launch.

## Features

- **Create a class** to add an empty class.
- **Create an event** for the selected class, for example `Oral exams round 1`.
- **Add a student** to the selected class.
- **Import a .txt list** to load multiple classes and their students.
- **Draw the next student** to randomly select a student who has not yet been
  drawn for the selected event. A student can only be drawn once per event;
  each new event starts with a fresh draw.

## Automatic saving

Classes, students, events, and drawn students are automatically saved in the
per-user application data folder. Your data is restored the next time you
start the app. Do not delete the data file if you want to keep your data.

## `.txt` file format

Write each class name inside square brackets, followed by one student per
line. Blank lines and lines beginning with `#` are ignored. A single file can
contain multiple classes:

```text
[Class 3A]
Alex Morgan
Jamie Taylor

[Class 3B]
Sam Jordan
Casey Parker
```

The entire file is read as UTF-8. If a student appears before a class heading,
the app reports the line number and does not import the file. Importing a class
that already exists appends its students to the existing list; duplicate names
are not removed.
