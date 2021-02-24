import norswap.autumn.Autumn;
import norswap.autumn.Grammar;
import norswap.autumn.ParseOptions;
import norswap.autumn.ParseResult;
import norswap.autumn.actions.ActionContext;
import norswap.autumn.positions.LineMapString;

public final class Parser extends Grammar {

    public rule root = seq(ws, alphanum).at_least(0);

    @Override
    public rule root() {
        return null;
    }
}