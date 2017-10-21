package org.lenskit.mooc.ii;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.Results;
import org.lenskit.util.math.Vectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemScorer extends AbstractItemScorer {
    private final SimpleItemItemModel model;
    private final DataAccessObject dao;
    private final int neighborhoodSize;

    @Inject
    public SimpleItemItemScorer(SimpleItemItemModel m, DataAccessObject dao) {
        model = m;
        this.dao = dao;
        neighborhoodSize = 20;
    }

    /**
     * Score items for a user.
     * @param user The user ID.
     * @param items The score vector.  Its key domain is the items to score, and the scores
     *               (rating predictions) should be written back to this vector.
     */
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        Long2DoubleMap itemMeans = model.getItemMeans();
        Long2DoubleMap ratings = getUserRatingVector(user);

        // TODO Normalize the user's ratings by subtracting the item mean from each one.
        for (Map.Entry<Long, Double> rating: ratings.entrySet()){
            rating.setValue(rating.getValue() - itemMeans.get(rating.getKey()));
        }
        List<Result> results = new ArrayList<>();

        for (Long item1: items ) {
            // TODO Compute the user's score for each item, add it to results
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
            // TODO Compute the neighbors with top N similarities
            Long2DoubleMap itemSimilairty = this.model.getNeighbors(item1);
            for (Map.Entry<Long, Double> entry : itemSimilairty.entrySet()){

            }
            for (Map.Entry<Long, Double> item2 : itemSimilairty.entrySet()){
                Result result2 = Results.create(item2.getKey(), item2.getValue());
                // only consider the item2 that is rated by current user
                if(! ratings.containsKey(result2.getId())) continue;
                if (pq.size()< this.neighborhoodSize){
                    pq.offer(result2);
                } else{
                    if (pq.peek().getScore() < result2.getScore()){
                        pq.poll();
                        pq.offer(result2);
                    }
                }
            }
            Long2DoubleMap neighbors = new Long2DoubleOpenHashMap();
            while (! pq.isEmpty()){
                Result r = pq.poll();
                neighbors.put(r.getId(), r.getScore());
            }
            //TODO calculate the weighted average
            double prod = Vectors.dotProduct(ratings, neighbors);
            double sumWeights = Vectors.sumAbs(neighbors);
            double similarity1 = 0.;
            if (itemMeans.containsKey(item1)){
                similarity1 = itemMeans.get(item1) + prod/ sumWeights;
            }
            results.add(Results.create(item1, similarity1));
        }

        return Results.newResultMap(results);

    }

    /**
     * Get a user's ratings.
     * @param user The user ID.
     * @return The ratings to retrieve.
     */
    private Long2DoubleOpenHashMap getUserRatingVector(long user) {
        List<Rating> history = dao.query(Rating.class)
                                  .withAttribute(CommonAttributes.USER_ID, user)
                                  .get();

        Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap();
        for (Rating r: history) {
            ratings.put(r.getItemId(), r.getValue());
        }

        return ratings;
    }


}
