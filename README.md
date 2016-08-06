# Team34_Final_Project_Maven_CodeU
Team 34 final project

export GOOGLE_APPLICATION_CREDENTIALS=/path/to/your-project-credentials.json

To run the program with Maven

mvn clean compile assembly:single
java -cp target/label-1.0-SNAPSHOT-jar-with-dependencies.jar com.codeu.team34.label.LabelApp demo-image.jpg

