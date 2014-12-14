JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
        Client.java \
        ClientImpl.java \
	ClientApp.java \
	dfsFile.java \
	Server.java \
	ServerImpl.java \
	ServerApp.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
