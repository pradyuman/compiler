LIB_ANTLR := lib/antlr.jar
ANTLR_SCRIPT := Micro.g4
CLASS_PATH := classes/

all: group compiler
group:
	@echo "Pradyuman Vig (pvig)  Tiger Cheng (tigerc)"
compiler:
	rm -rf build
	mkdir -p build/main
	java -cp $(LIB_ANTLR) org.antlr.v4.Tool -o build/main -package main $(ANTLR_SCRIPT)
	rm -rf classes
	mkdir -p classes
	javac -cp $(LIB_ANTLR) -d classes src/main/*.java src/main/utils/*.java build/main/*.java
lexer:
	@java -cp "$(LIB_ANTLR):$(CLASS_PATH)" \
	org.antlr.v4.gui.TestRig main.Micro tokens -tokens
run:
	@java -cp "$(LIB_ANTLR):$(CLASS_PATH)" \
	main.Micro testcases_step5/input/$(FILE).micro > $(FILE).test
check:
	diff -b -B testcases/output/$(FILE).out $(FILE).test
download:
	curl -O https://engineering.purdue.edu/EE468/project/step5/testcases_step5.tar.gz
	tar -xvzf testcases_step5.tar.gz
run-tiny:
	lib/tiny $(FILE)
test: run check
testall:
	./scripts/testall.sh
clean:
	rm -rf classes build

.PHONY: all group compiler clean
