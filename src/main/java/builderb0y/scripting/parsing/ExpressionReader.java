package builderb0y.scripting.parsing;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.function.CharPredicate;

/**
lowest-level parsing logic for scripts,
handles reading of characters, operators, and identifiers,
skipping of whitespace, and maintains an up-to-date
line number for the current cursor position.

for higher-level parsing logic, see {@link ExpressionParser}.
*/
@SuppressWarnings("deprecation")
public class ExpressionReader {

	public final String input;
	public int cursor, line, column;

	public ExpressionReader(String input) {
		this.input = canonicalizeLineEndings(input);
		this.line = 1;
		this.column = 1;
	}

	/** replaces "\r" and "\r\n" with "\n". */
	public static String canonicalizeLineEndings(String input) {
		int length = input.length();
		StringBuilder builder = new StringBuilder(length);
		for (int index = 0; index < length;) {
			char c = input.charAt(index);
			if (c == '\r') {
				index++;
				if (index < length && input.charAt(index) == '\n') {
					index++;
				}
				builder.append('\n');
			}
			else {
				builder.append(c);
				index++;
			}
		}
		return builder.toString();
	}

	@Deprecated
	public char getChar(int index) {
		return index < this.input.length() ? this.input.charAt(index) : 0;
	}

	@Deprecated
	public boolean canRead() {
		return this.cursor < this.input.length();
	}

	public boolean canReadAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.canRead();
	}

	/**
	called after a char is read. updates {@link #cursor}, {@link #line},
	and {@link #column} based on the character which was read.
	*/
	public void onCharRead(char c) {
		this.cursor++;
		if (c == '\n') {
			this.line++;
			this.column = 1;
		}
		else {
			this.column++;
		}
	}

	/**
	called after multiple chars are read. updates {@link #cursor}, {@link #line},
	and {@link #column} based on the characters which were read.
	*/
	public void onCharsRead(String s) {
		for (int index = 0, length = s.length(); index < length; index++) {
			this.onCharRead(s.charAt(index));
		}
	}

	/**
	reads one character, and returns that character.
	returns 0 ('\0' or '\u0000') if there are no more
	characters to read because we reached the end of the input.
	{@link #cursor}, {@link #line}, and {@link #column}
	are updated based on the character which was read.
	*/
	@Deprecated
	public char read() throws ScriptParsingException {
		if (this.canRead()) {
			char c = this.input.charAt(this.cursor);
			this.onCharRead(c);
			if (c == 0) {
				throw new ScriptParsingException("Encountered NUL character in input", this);
			}
			return c;
		}
		return 0;
	}

	public char readAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.read();
	}

	/**
	returns the next character which would be read by {@link #read()},
	without actually reading it. {@link #cursor}, {@link #line}, and
	{@link #column} are left unchanged as a result of calling this method.
	*/
	@Deprecated
	public char peek() {
		return this.canRead() ? this.input.charAt(this.cursor) : 0;
	}

	public char peekAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.peek();
	}

	/**
	attempts to read one character, returns true if successful,
	and false if we reached the end of our input.
	*/
	@Deprecated
	public boolean skip() throws ScriptParsingException {
		return this.read() != 0;
	}

	/**
	attempts to skip (count) characters.
	stops if the end of input has been reached,
	or is reached in the process of skipping.
	returns the number of characters which were actually skipped.
	the returned number will be less than (count)
	if the end of input was reached while skipping.
	*/
	@Deprecated
	public int skip(int count) {
		count = Math.min(count, this.input.length() - this.cursor);
		for (int i = 0; i < count; i++) {
			this.onCharRead(this.input.charAt(this.cursor));
		}
		return count;
	}

	/**
	attempts to read (count) characters.
	stops if the end of input has been reached,
	or is reached in the process of reading.
	returns the characters that were read, as a String.
	the returned String's {@link String#length()} will be less
	than (count) if the end of input was reached while reading.
	*/
	@Deprecated
	public String read(int count) {
		int startIndex = this.cursor;
		int endIndex = Math.min(startIndex + count, this.input.length());
		String read = this.input.substring(startIndex, endIndex);
		this.onCharsRead(read);
		return read;
	}

	/**
	if the next character to be read is (expected),
	reads that character, and returns true.
	otherwise, does NOT read that character, and returns false.
	*/
	@Deprecated
	public boolean has(char expected) {
		if (!this.canRead()) return false;
		char got = this.input.charAt(this.cursor);
		if (got != expected) return false;
		this.onCharRead(got);
		return true;
	}

	public boolean hasAfterWhitespace(char expected) throws ScriptParsingException {
		this.skipWhitespace();
		return this.has(expected);
	}

	/**
	if the next characters to be read are (expected),
	reads those characters and returns true.
	otherwise, does NOT read those characters, and returns false.
	*/
	@Deprecated
	public boolean has(String expected) {
		if (this.input.regionMatches(this.cursor, expected, 0, expected.length())) {
			this.onCharsRead(expected);
			return true;
		}
		return false;
	}

	public boolean hasAfterWhitespace(String expected) throws ScriptParsingException {
		this.skipWhitespace();
		return this.has(expected);
	}

	/**
	if the next character to be read tests true by the
	provided predicate, reads that character and returns true.
	otherwise, does NOT read that character, and returns false.
	*/
	@Deprecated
	public boolean has(CharPredicate predicate) {
		if (!this.canRead()) return false;
		char got = this.input.charAt(this.cursor);
		if (!predicate.test(got)) return false;
		this.onCharRead(got);
		return true;
	}

	public boolean hasAfterWhitespace(CharPredicate predicate) throws ScriptParsingException {
		this.skipWhitespace();
		return this.has(predicate);
	}

	/**
	while the next character to be read tests true
	by th provided predicate, skips that character.
	*/
	@Deprecated
	public void skipWhile(CharPredicate predicate) {
		char c;
		while (this.canRead() && predicate.test(c = this.input.charAt(this.cursor))) {
			this.onCharRead(c);
		}
	}

	/**
	while the next character to be read tests true
	by th provided predicate, reads that character.
	returns the characters which were read.
	*/
	@Deprecated
	public String readWhile(CharPredicate predicate) {
		int start = this.cursor;
		this.skipWhile(predicate);
		return this.input.substring(start, this.cursor);
	}

	/**
	skips whitespace, and comments.
	comments are defined as starting with a semicolon,
	and ending with a newline character.
	this method should only be called
	outside of a string literal context.
	*/
	public void skipWhitespace() throws ScriptParsingException {
		while (true) {
			this.skipWhile(Character::isWhitespace);
			if (this.has(';')) {
				int end;
				if (this.has('(')) {
					int depth = 1;
					while (depth > 0) {
						char read = this.read();
						if (read == '(') depth++; else
						if (read == ')') depth--; else
						if (read ==  0 ) throw new ScriptParsingException("Mismatched parentheses in comment", this);
					}
				}
				else if (this.has(';')) {
					end = this.input.indexOf(";;", this.cursor);
					if (end < 0) throw new ScriptParsingException("Un-terminated multi-line comment", this);
					this.skip(end + 2 - this.cursor);
				}
				else {
					end = this.input.indexOf('\n', this.cursor);
					if (end < 0) end = this.input.length();
					this.skip(end + 1 - this.cursor);
				}
			}
			else {
				break;
			}
		}
	}

	/**
	if the next character to be read is (expected),
	reads that character and returns normally.
	otherwise, does NOT read that character,
	and throws a {@link ScriptParsingException}.
	*/
	@Deprecated
	public void expect(char expected) throws ScriptParsingException {
		if (!this.has(expected)) {
			throw new ScriptParsingException("Expected '" + expected + '\'', this);
		}
	}

	public void expectAfterWhitespace(char expected) throws ScriptParsingException {
		this.skipWhitespace();
		this.expect(expected);
	}

	/**
	if the next characters to be read are (expected),
	reads those characters and returns normally.
	otherwise, does NOT read those characters,
	and throws a {@link ScriptParsingException}.
	*/
	@Deprecated
	public void expect(String expected) throws ScriptParsingException {
		if (!this.has(expected)) {
			throw new ScriptParsingException("Expected '" + expected + '\'', this);
		}
	}

	public void expectAfterWhitespace(String expected) throws ScriptParsingException {
		this.skipWhitespace();
		this.expect(expected);
	}

	/**
	reads an operator as if by {@link #readOperator()},
	but does not modify {@link #cursor}, {@link #line}, or {@link #column}.
	*/
	@Deprecated
	public String peekOperator() {
		CursorPos old = this.getCursor();
		String operator = this.readOperator();
		this.setCursor(old);
		return operator;
	}

	public String peekOperatorAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.peekOperator();
	}

	/**
	reads an operator.
	operators are defined as one or more operator symbols,
	see {@link #isOperatorSymbol(char)} for the exact predicate used
	to determine whether or not a character is an operator symbol.
	*/
	@Deprecated
	public String readOperator() {
		return this.readWhile(ExpressionReader::isOperatorSymbol);
	}

	public String readOperatorAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.readOperator();
	}

	/**
	returns true if the provided character counts as an operator character.
	operator characters include the following:
		!#%&*+,-./:<=>?@\^|~
	*/
	public static boolean isOperatorSymbol(char c) {
		return switch (c) {
			case '!', '#', '$', '%', '&', '*', '+', ',', '-', '.', '/', ':', '<', '=', '>', '?', '@', '\\', '^', '|', '~' -> true;
			default -> false;
		};
	}

	@Deprecated
	public boolean hasOperator(String operator) {
		CursorPos revert = this.getCursor();
		if (this.has(operator) && !isOperatorSymbol(this.peek())) {
			return true;
		}
		else {
			this.setCursor(revert);
			return false;
		}
	}

	public boolean hasOperatorAfterWhitespace(String operator) throws ScriptParsingException {
		this.skipWhitespace();
		return this.hasOperator(operator);
	}

	@Deprecated
	public void expectOperator(String operator) throws ScriptParsingException {
		if (!this.hasOperator(operator)) throw new ScriptParsingException("Expected '" + operator + '\'', this);
	}

	public void expectOperatorAfterWhitespace(String operator) throws ScriptParsingException {
		this.skipWhitespace();
		this.expectOperator(operator);
	}

	public static boolean isLetterOrUnderscore(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_');
	}

	public static boolean isLetterNumberOrUnderscore(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_');
	}

	@Deprecated
	public @Nullable String readIdentifierOrNull() throws ScriptParsingException {
		char c = this.peek();
		if (isLetterOrUnderscore(c)) {
			int startIndex = this.cursor;
			do {
				this.onCharRead(c);
				c = this.peek();
			}
			while (isLetterNumberOrUnderscore(c));
			return this.input.substring(startIndex, this.cursor);
		}
		else if (c == '`') {
			this.onCharRead('`');
			CursorPos start = this.getCursor();
			while (true) {
				c = this.peek();
				if (c == 0 || c == '\n') {
					this.setCursor(start);
					throw new ScriptParsingException("Un-terminated escaped identifier", this);
				}
				if (c == '`') {
					this.onCharRead('`');
					break;
				}
				this.onCharRead(c);
			}
			return this.input.substring(start.cursor, this.cursor - 1);
		}
		else {
			return null;
		}
	}

	public @Nullable String readIdentifierOrNullAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.readIdentifierOrNull();
	}

	@Deprecated
	public String readIdentifier() throws ScriptParsingException {
		String identifier = this.readIdentifierOrNull();
		return identifier != null ? identifier : "";
	}

	public String readIdentifierAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.readIdentifier();
	}

	@Deprecated
	public String peekIdentifier() throws ScriptParsingException {
		CursorPos revert = this.getCursor();
		String identifier = this.readIdentifier();
		this.setCursor(revert);
		return identifier;
	}

	public String peekIdentifierAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.peekIdentifier();
	}

	@Deprecated
	public boolean hasIdentifier(String identifier) throws ScriptParsingException {
		char c = this.peek();
		if (isLetterOrUnderscore(c)) {
			if (
				this.input.regionMatches(this.cursor, identifier, 0, identifier.length()) &&
				!isLetterNumberOrUnderscore(this.getChar(this.cursor + identifier.length()))
			) {
				this.onCharsRead(identifier);
				return true;
			}
		}
		else if (c == '`') {
			if (
				this.input.regionMatches(this.cursor + 1, identifier, 0, identifier.length()) &&
				this.getChar(this.cursor + identifier.length() + 2) == '`'
			) {
				this.onCharRead('`');
				this.onCharsRead(identifier);
				this.onCharRead('`');
				return true;
			}
		}
		return false;
	}

	public boolean hasIdentifierAfterWhitespace(String identifier) throws ScriptParsingException {
		this.skipWhitespace();
		return this.hasIdentifier(identifier);
	}

	@Deprecated
	public String expectIdentifier() throws ScriptParsingException {
		String identifier = this.readIdentifier();
		if (!identifier.isEmpty()) return identifier;
		else throw new ScriptParsingException("Expected identifier", this);
	}

	public String expectIdentifierAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.expectIdentifier();
	}

	@Deprecated
	public void expectIdentifier(String identifier) throws ScriptParsingException {
		if (!this.hasIdentifier(identifier)) {
			throw new ScriptParsingException("Expected '" + identifier + '\'', this);
		}
	}

	public void expectIdentifierAfterWhitespace(String identifier) throws ScriptParsingException {
		this.skipWhitespace();
		this.expectIdentifier(identifier);
	}

	public String getSource() {
		return this.input;
	}

	public String getSourceForError() {
		//grab the last 10 lines leading up to the error.
		int start = this.cursor;
		for (int loop = 0; loop < 10; loop++) {
			start = this.input.lastIndexOf('\n', start - 1);
			if (start < 0) return this.input.substring(0, this.cursor);
		}
		return this.input.substring(start + 1, this.cursor);
	}

	public void unread() {
		int newCursor = this.cursor - 1;
		char c = this.input.charAt(newCursor);
		if (c == '\n') throw new IllegalStateException("Cannot unread newline");
		this.cursor = newCursor;
		this.column--;
	}

	public CursorPos getCursor() {
		return new CursorPos(this.cursor, this.line, this.column);
	}

	public void setCursor(CursorPos position) {
		this.cursor = position.cursor;
		this.line   = position.line  ;
		this.column = position.column;
	}

	public static record CursorPos(int cursor, int line, int column) {}
}