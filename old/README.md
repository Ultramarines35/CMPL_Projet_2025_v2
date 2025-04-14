## Getting Started

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

## Lauch the project

Run the following commands :

Generate `projetLexer.java` and `projetParser.java`

```bash
java -cp ./lib/antlr-3.5.2-complete.jar org.antlr.Tool src/projet.g
mv -f *.java src/
```

Compile in bin folder the project .java files

```bash
javac -cp ./lib/* -d bin src/*.java
```

Run the compiled project with the main function in projet.java

```bash
java -cp "lib/*;bin" projet
```

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).
