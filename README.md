# COS341 Prac

Takes a plain-text `SPL.txt` file and
produces either a syntax error with hints, or a `tree.xml` file containing
the program's full syntax tree.

## What this does

1. **Lex**: convert raw SPL source into a token stream (`NUM`,
   `USER-DEFINED-NAME`, `STRING`, keywords/symbols).
2. **Parse**: run the token stream through an LR shift-reduce parser
   (the SPL grammar is not LL(1), so we use a lexer + LR table approach
   rather than a lexer-less or recursive-descent parser).
3. **Emit**: write out `tree.xml`, where every node (root/inner/leaf) has
   a unique ID, a `contents` field, a `children` list, and (for non-root
   nodes) a `parent` ID.

## Requirements

- Java (JDK 17+)
- Maven

## Build & run

```bash
mvn clean package
java -jar target/compiler.jar SPL.txt
```

On success, `tree.xml` is written to the project root (or wherever `Main`
is configured to write it). On a syntax error, the parser prints a
message identifying the offending token, its line number, and what was
expected instead.

## Project structure

```
spl-compiler/
├── SPL.txt
├── src/main/java/spl/
│   ├── Main.java                # entry point: SPL.txt -> tree.xml
│   ├── model/                   # shared contracts: Token, TreeNode, etc.
│   ├── lexer/                   # tokenizer
│   ├── grammar/                 # grammar rules + LR parsing table
│   ├── parser/                  # shift-reduce driver
│   └── output/                  # tree.xml writer
└── src/test/java/spl/           # unit + integration tests, mirrors main
```

See `model/Token.java` and `model/TreeNode.java` for the shared data
contracts every module builds against — check these before changing
anything that crosses module boundaries.

## Team / ownership

| Area | Owner | Package |
|---|---|---|
| Lexer | Sambulo | `spl.lexer` |
| Grammar & LR table | TBD | `spl.grammar` |
| Parser engine | TBD | `spl.parser` |
| Tree output & testing | TBD | `spl.output`, `src/test` |

## Grammar reference

The full SPL lexical spec and context-free grammar are in
`Prac-Spec-Syntax.pdf`. Key points:

- Every token must end in a blank space (ASCII 32 or 13).
- `USER-DEFINED-NAME` tokens always start with `#`, so there's no
  conflict with reserved keywords.
- The grammar is **not** LL(1), so we build an SLR(1) parsing table
  instead.

## Testing
(we'll see how well things are going, then decide if we add these or not)
```bash
mvn test
```

- `LexerTest` — token classification, including malformed-input cases.
- `ParsingTableTest` — checks for shift/reduce or reduce/reduce
  conflicts, especially around the grammar's nullable productions
  (`V_DECL`, `F_DECL`, `ALGO`).
- `ParserTest` — shift/reduce driving logic on small token sequences.
- `XmlTreeWriterTest` — output format checks against a mock tree.
- `EndToEndTest` — full pipeline test: sample `SPL.txt` in, `tree.xml`
  out, diffed against an expected tree.

## Notes

- The tutors only check the output `tree.xml`, not the parser's source
  code — but keep the code clean anyway, since it makes debugging test
  failures far easier.
- Unique node IDs matter beyond this practical: later phases link tree
  nodes to a semantic data table (name-scope analysis, type-checking)
  using these IDs as foreign keys.
