package org.lenskit.mooc.ii;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemBasedItemScorer;
import org.lenskit.results.Results;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

/**
 * Global item scorer to find similar items.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */

public class SimpleItemBasedItemScorer extends AbstractItemBasedItemScorer {
    private final SimpleItemItemModel model;

    @Inject
    public SimpleItemBasedItemScorer(SimpleItemItemModel mod) {
        model = mod;
    }

    /**
     * Score items with respect to a set of reference items.
     * @param basket The reference items.
     * @param items The score vector. Its domain is the items to be scored, and the scores should
     *               be stored into this vector.
     */
    @Override
    public ResultMap scoreRelatedItemsWithDetails(@Nonnull Collection<Long> basket, Collection<Long> items) {
        List<Result> results = new ArrayList<>();

        // TODO Score the items and put them in results
        Long2DoubleMap itemMeans = new Long2DoubleOpenHashMap();
        for (Long item1 : items){
            Long2DoubleMap itemSimilairty = this.model.getNeighbors(item1);
            // TODO Compute the n highest-scoring items from candidates
            PriorityQueue<Result> pq = new PriorityQueue<>(new Comparator<Result>() {
                @Override
                public int compare(Result o1, Result o2) {
                    if (o1.getScore() < o2.getScore() ){
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
            for (Map.Entry<Long, Double> item2 : itemSimilairty.entrySet()){
                if (pq.size()< )
            }

            if (model.hasItem(refItem)) {
                for (long candidate : candidates) {
                    double candidateScore = model.getItemAssociation(refItem, candidate);
                    if(pq.size() <n) {
                        pq.offer(Results.create(candidate, candidateScore));
                    } else{
                        if (pq.peek().getScore() < candidateScore){
                            pq.poll();
                            pq.offer(Results.create(candidate, candidateScore));
                        }
                    }
                }
            }
            while (!pq.isEmpty()){
                Result r =pq.poll();
                results.add(r);
            }

            Collections.reverse(results);

        }
        itemMeans = this.model.getItemMeans();




        return Results.newResultMap(results);
    }
}
