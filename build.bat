javac -d ./.class --class-path=./.class --module-path ".\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls common/Transaction.java
javac -d ./.class --class-path=./.class --module-path ".\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls common/Group.java
javac -d ./.class --class-path=./.class --module-path ".\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls common/Crypto.java
javac -d ./.class --class-path=./.class --module-path ".\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls common/Compression.java
javac -d ./.class --class-path=./.class --module-path ".\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls common/DispAlert.java
javac -d ./.class --class-path=./.class --module-path ".\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls client/Main.java
javac -d ./.class --class-path=./.class --module-path ".\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls server/Server.java
javac -d ./.class --class-path=./.class --module-path ".\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls client/GroupCreatePopup.java
javac -d ./.class --class-path=./.class --module-path ".\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls client/GroupEditPopup.java
cd .class
jar -cvef client.Main client.jar common client ../common/styles.css
jar -cvef server.Server server.jar common server ../common/styles.css