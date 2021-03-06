LIB := "lib/*"
LIB_ANTLR := lib/antlr.jar
ANTLR_SCRIPT := Micro.g4
CLASS_PATH := classes/

all: group compiler
group:
	@echo "Pradyuman Vig (pvig)  Tiger Cheng (tigerc)"
compiler:
	rm -rf build
	mkdir -p build/compiler
	java -cp $(LIB_ANTLR) org.antlr.v4.Tool -o build/compiler -package compiler $(ANTLR_SCRIPT)
	rm -rf classes
	mkdir -p classes
	javac -cp $(LIB) -d classes src/compiler/*.java src/compiler/element/*.java src/compiler/expression/*.java src/compiler/translator/*.java build/compiler/*.java
lexer:
	@java -cp "$(LIB):$(CLASS_PATH)" \
	org.antlr.v4.gui.TestRig compiler.Micro tokens -tokens
run:
	@java -cp "$(LIB):$(CLASS_PATH)" \
	compiler.Micro $(FILE).micro > $(FILE).test
check:
	diff -b -B $(FILE).out $(FILE).test
download:
	curl -O https://engineering.purdue.edu/EE468/project/step5/testcases_step5.tar.gz
	tar -xvzf testcases_step5.tar.gz
run-tiny:
	lib/tiny $(FILE).test > $(FILE).tout
check-tiny:
	bash -c 'diff -b -B <(head -n 1 $(IFILE)) <(head -n 1 $(CFILE))'
test: run check
testall:
	./scripts/testall.sh
clean:
	rm -rf classes build

.PHONY: all group compiler clean
