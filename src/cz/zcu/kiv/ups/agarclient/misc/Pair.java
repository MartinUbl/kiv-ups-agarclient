package cz.zcu.kiv.ups.agarclient.misc;

public class Pair<A> implements Comparable<Pair<A>>
{
    public A first;
    public A second;

    public Pair(A frst, A secnd)
    {
        first = frst;
        second = secnd;
    }

    @Override
    public int compareTo(Pair<A> a)
    {
        return (a.first.equals(first) && a.second.equals(second)) ? 0 : 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object a)
    {
        if (a instanceof Pair)
            return ((Pair<A>)a).first.equals(first) && ((Pair<A>)a).second.equals(second);
        else
            return false;
    }
}
