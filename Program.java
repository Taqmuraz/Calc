import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

interface TokenStream
{
    // contract : end = null
    String next();

    default TokenStream append(String...tokens)
    {
        return new AppendTokenStream(this, tokens);
    }
    default TokenStream prepend(String...tokens)
    {
        return new PrependTokenStream(this, tokens);
    }
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
    Value nextValue(TokenStream stream)
    {
        String token = stream.next();
        if (Character.isDigit(token.charAt(0)))
        {
            return new NumberSyntax().value(stream.prepend(token));
        }
        if (token.equals("("))
        {
            return new BraceSyntax().value(stream.prepend(token));
        }
        return () -> 0f;
    }

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
            if (operations.containsKey(token))
            {
                Value second = nextValue(stream);
                value = operations.get(token).value(value, second);
            }
            else
            {
                value = nextValue(stream.prepend(token));
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
        stream.next();
        return new ExpressionSyntax().value(stream);
    }
}

class GlobalSyntax implements Syntax
{
    @Override
    public Value value(TokenStream stream)
    {
        return new BraceSyntax().value(stream.prepend("(").append(")"));
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

class AppendTokenStream implements TokenStream
{
    int position;
    String[] tokens;
    TokenStream stream;

    public AppendTokenStream(TokenStream stream, String... tokens)
    {
        this.stream = stream;
        this.tokens = tokens;
    }

    @Override
    public String next()
    {
        if (position < tokens.length)
        {
            String token = stream.next();
            if (token == null) return tokens[position++];
            return token;
        }
        return null;
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
        System.out.println(new GlobalSyntax().value(stream).get());
    }
}