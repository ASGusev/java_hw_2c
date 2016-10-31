import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Collections {
    public static <SourceType, DestType> List < DestType > map (Iterable<SourceType> iterable,
                                                                Function1<? super SourceType, DestType> function) throws Exception {
        List<DestType> resList = new LinkedList<DestType>();
        for (SourceType curElement: iterable) {
            resList.add(function.apply(curElement));
        }
        return resList;
    }

    public static <T> List<T> filter(Iterable<T> iterable,
                                     Predicate<? super T> predicate) throws Exception{
        List<T> trueList = new LinkedList<T>();
        for (T curElement : iterable) {
            if (predicate.apply(curElement)) {
                trueList.add(curElement);
            }
        }
        return trueList;
    }

    public static <T> List<T> takeWhile(Iterable<T> iterable,
                                        Predicate<? super T> predicate) throws Exception {
        List<T> resList = new LinkedList<T>();
        Iterator<T> it = iterable.iterator();
        while (it.hasNext()) {
            T curElem = it.next();
            if (!predicate.apply(curElem)) {
                break;
            }
            resList.add(curElem);
        }
        return resList;
    }

    public static <T> List<T> takeUnless(Iterable<T> iterable, Predicate<? super T>
            predicate) throws Exception {
        return takeWhile(iterable, predicate.not());
    }

    public static <SourceType, DestType> DestType foldl(
            Function2<? super DestType,? super SourceType,DestType> function,
            DestType init, Iterable<SourceType> iterable) throws Exception {
        DestType res = init;
        for (SourceType elem : iterable) {
            res = function.apply(res, elem);
        }
        return res;
    }

    public static <SourceType, DestType> DestType foldr(Function2<? super SourceType, ?super DestType, DestType> function,
                                                        DestType init, Iterable<SourceType> iterable) throws Exception {
        return foldrImpl(function, init, iterable.iterator());
    }

    private static <SourceType, DestType> DestType foldrImpl(Function2<? super SourceType, ? super DestType, DestType> function,
                                                             DestType init, Iterator<SourceType> iterator) throws Exception {
        if (iterator.hasNext()) {
            SourceType curElem = iterator.next();
            return function.apply(curElem, foldrImpl(function, init, iterator));
        }
        return init;
    }
}
