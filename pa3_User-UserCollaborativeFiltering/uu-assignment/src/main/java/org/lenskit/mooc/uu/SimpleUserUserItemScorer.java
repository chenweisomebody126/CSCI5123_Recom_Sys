package org.lenskit.mooc.uu;

import it.unimi.dsi.fastutil.doubles.DoubleHeaps;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.entities.CommonTypes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.util.math.Vectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

/**
 * User-user item scorer.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleUserUserItemScorer extends AbstractItemScorer {
    private final DataAccessObject dao;
    private final int neighborhoodSize;

    /**
     * Instantiate a new user-user item scorer.
     * @param dao The data access object.
     */
    @Inject
    public SimpleUserUserItemScorer(DataAccessObject dao) {
        this.dao = dao;
        neighborhoodSize = 30;
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        // TODO Score the items for the user with user-user CF
        //retrieve the rating vector of input user and subtract its mean
        Long2DoubleOpenHashMap r_u = getUserRatingVector(user);
        Long2DoubleOpenHashMap mean_centered_r_u = subtracting_mean_rating(r_u);
        double mean_r_u = Vectors.mean(r_u);


        //iterate over all users except itself to calculate each user's mean-centered rating vectors
        LongSet users = dao.getEntityIds(CommonTypes.USER);

        Long2DoubleOpenHashMap mean_centered_ratings = new Long2DoubleOpenHashMap();
        Long2DoubleOpenHashMap users_mean_ratings = new Long2DoubleOpenHashMap();

        //initialize a heap for each item to get top N cos similarities
        HashMap<Long, PriorityQueue<Result>> cos =  new HashMap<>();
        for (long itemId: items){
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

            cos.put(itemId, pq);
        }

        for (long user_each : users) {
            if (user_each == user) continue;
            Long2DoubleOpenHashMap r_v = getUserRatingVector(user_each);
            for (long item: items){
                if (!r_v.containsKey(item)) continue;
                //calculate the cos similarity
                double mean_r_v = Vectors.mean(r_v);

                cos.get(item).
            }
            double mean_user = Vectors.mean(r_v);

            addScalar(r_vec, mean_user*(-1.0));
            mean_centered_ratings.put(r_vec.getItemId(), r_vec.getValue());
            users_mean_ratings.put(user_each, mean_user);
        }
        //user Vectors class to calculate the cosine of mean-centered rating vectors after filtering who rated this item
        for (Long item: items){
            for (Long user_any: users){
                if (user_any == user) continue;
                if ()
            }
        }
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Get a user's rating vector.
     * @param user The user ID.
     * @return The rating vector, mapping item IDs to the user's rating
     *         for that item.
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

    private Long2DoubleOpenHashMap subtracting_mean_rating(Long2DoubleOpenHashMap ratings_vec){
        double mean_r_u = Vectors.mean(ratings_vec);
        Long2DoubleOpenHashMap mean_centered_ratings_vec = new Long2DoubleOpenHashMap();
        Iterator<Long2DoubleMap.Entry> iter = Vectors.fastEntryIterator(ratings_vec);
        while (iter.hasNext()){
            Long2DoubleMap.Entry itemId_rating = iter.next();
            mean_centered_ratings_vec.put(itemId_rating.getLongKey(), itemId_rating.getDoubleValue() - mean_r_u);
        }
        return mean_centered_ratings_vec;
    }

}
