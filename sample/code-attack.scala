import java.io.{BufferedReader, File, InputStreamReader}

val command = "ls -al /etc/ssl"

val child = Runtime.getRuntime.exec(command)

val input = new BufferedReader(new InputStreamReader(child.getInputStream))
var line: String = input.readLine

while (line != null) {
  println("Output from ls: " + line)
  line = input.readLine
}