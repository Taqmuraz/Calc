import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

interface TokenStream
{
    String next();
}

interface Value
{
    double get();
}

interface Syntax
{
    Value value(TokenStream stream);
}

class NumberSyntax implements Syntax
{
    @Override
    public Value value(TokenStream stream)
    {
        double v = Double.valueOf(stream.next());
        return () -> v;
    }
}

interface BinaryExpression
{
    Value value(Value a, Value b);
}

class ExpressionSyntax implements Syntax
{
    @Override
    public Value value(TokenStream stream)
    {
        String token = stream.next();
        Value value = () -> 0f;

        Map<String, BinaryExpression> operations = Map.<String,BinaryExpression>ofEntries(
            Map.entry("+", (a, b) -> () -> a.get() + b.get()),
            Map.entry("-", (a, b) -> () -> a.get() - b.get()),
            Map.entry("*", (a, b) -> () -> a.get() * b.get()),
            Map.entry("/", (a, b) -> () -> a.get() / b.get())
        );

        while(!token.equals(")"))
        {
            if (Character.isDigit(token.charAt(0)))
            {
                value = new NumberSyntax().value(new PrependTokenStream(stream, token));
            }
            else if (operations.containsKey(token))
            {
                Value second = new NumberSyntax().value(stream);
            }

            token = stream.next();
        }

        return value;
    }
}

class BraceSyntax implements Syntax
{
    @Override
    public Value value(TokenStream stream)
    {
        return new ExpressionSyntax().value(stream);
    }
}

class BufferedTokenStream implements TokenStream
{
    int position;
    String[] buffer;

    public BufferedTokenStream(String[] buffer)
    {
        this.buffer = buffer;
    }

    @Override
    public String next()
    {
        return position < buffer.length ? buffer[position++] : null;
    }
}

class PrependTokenStream implements TokenStream
{
    int position;
    String[] tokens;
    TokenStream stream;

    public PrependTokenStream(TokenStream stream, String... tokens)
    {
        this.stream = stream;
        this.tokens = tokens;
    }

    @Override
    public String next()
    {
        if (position < tokens.length) return tokens[position++];
        else return stream.next();
    }
}

interface SymbolKind
{
    int kind(char symbol);
}

class ClassicSymbolKind implements SymbolKind
{
    int braceKind = 4;

    @Override
    public int kind(char symbol)
    {
        if (Character.isLetter(symbol)) return 0;
        if (Character.isDigit(symbol)) return 1;
        if (Character.isSpaceChar(symbol)) return 2;
        if (symbol == '(') return braceKind++;
        if (symbol == ')') return braceKind--;
        return 3;
    }
}

interface TokenReader
{
    TokenStream read(String line);
}

class StandardTokenReader implements TokenReader
{
    SymbolKind symbolKind;

    public StandardTokenReader(SymbolKind symbolKind)
    {
        this.symbolKind = symbolKind;
    }

    @Override
    public TokenStream read(String line)
    {
        List<String> tokens = new ArrayList<String>();
        String token = String.valueOf(line.charAt(0));

        for (int i = 1; i < line.length(); i++)
        {
            int kind = symbolKind.kind(line.charAt(i));
            int lastKind = symbolKind.kind(line.charAt(i - 1));
            if (kind != lastKind)
            {
                tokens.add(token);
                token = "";
            }
            token += line.charAt(i);
        }
        if (!token.isEmpty()) tokens.add(token);

        return new BufferedTokenStream(tokens.stream().filter(t -> !t.isBlank()).toArray(String[]::new));
    }
}

public class Program
{
    public static void main(String[] args)
    {
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        scanner.close();
        TokenStream stream = new StandardTokenReader(new ClassicSymbolKind()).read(input);
        String token;
        do
        {
            token = stream.next();
            System.out.println(token);
        }
        while(token != null);
    }
}