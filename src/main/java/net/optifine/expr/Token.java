package net.optifine.expr;

public record Token(TokenType type, String text) {
	public String toString() {
		return this.text;
	}
}
