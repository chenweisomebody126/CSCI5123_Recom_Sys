package org.lenskit.mooc.uu;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.entities.CommonTypes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.Results;
import org.lenskit.util.math.Vectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

/**
 * User-user item scorer.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class
SimpleUserUserItemScorer extends AbstractItemScorer {
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

        Long2DoubleOpenHashMap user_ratings = getUserRatingVector(user);
        double user_mean = Vectors.mean(user_ratings);
        for (Map.Entry<Long,Double> r : user_ratings.entrySet()) {
            long u_key = r.getKey();
            double standardize = r.getValue() - user_mean;
            user_ratings.put(u_key, standardize);
        }
        double user_norm = Vectors.euclideanNorm(user_ratings);


        /* calculate all cosines */
        LongSet all_ids = dao.getEntityIds(CommonTypes.USER);
        HashMap<Long,Double> cosines = new HashMap<>();
        for (long id : all_ids) {
            if (user != id) {
                Long2DoubleOpenHashMap neighbor_ratings = getUserRatingVector(id);

                double neighbor_mean = Vectors.mean(neighbor_ratings);
                for (Map.Entry<Long,Double> r : neighbor_ratings.entrySet()) {
                    long neighbor_key = r.getKey();
                    double standardize = r.getValue() - neighbor_mean;
                    neighbor_ratings.put(neighbor_key, standardize);
                }
                double norm1 = Vectors.euclideanNorm(neighbor_ratings);
                double product = Vectors.dotProduct(neighbor_ratings, user_ratings);
                cosines.put(id,product/norm1/user_norm);
            }
        }


        /* sort all cosines in descending order */
        LinkedHashMap<Long,Double> sorted_cosines = new LinkedHashMap<>();
        while (!cosines.isEmpty()) {
            double cos_val = -2.0;
            long id = 0;
            for (Map.Entry<Long,Double> r : cosines.entrySet()) {
                if (r.getValue() > cos_val) {
                    cos_val = r.getValue();
                    id = r.getKey();
                }
            }
            sorted_cosines.put(id, cos_val);
            cosines.remove(id);
        }

        /* create top 30 cosines for each item */
        LinkedHashMap<Long,LinkedHashMap<Long,Double>> top30cosines = new LinkedHashMap<>();
        for (long item : items) {
            LinkedHashMap<Long,Double> topcosines = new LinkedHashMap<>();
            for (Map.Entry<Long,Double> r : sorted_cosines.entrySet()) {
                if (topcosines.size() == neighborhoodSize) {
                    top30cosines.put(item, topcosines);
                    break;
                }
                if (getUserRatingVector(r.getKey()).containsKey(item)) {
                    if (r.getValue() <= 0) {
                        break;
                    }
                    topcosines.put(r.getKey(),r.getValue());
                }
            }
            if (topcosines.size() < neighborhoodSize && topcosines.size() > 1) {
                top30cosines.put(item, topcosines);
            }
        }

        /* compute the summed norm for each top30cosine  */
        LinkedHashMap<Long,Double> top30norms = new LinkedHashMap<>();
        for (Map.Entry<Long,LinkedHashMap<Long,Double>> r1 : top30cosines.entrySet()) {
            double cos_norm = 0.0;
            for (Map.Entry<Long,Double> r2 : r1.getValue().entrySet()) {
                cos_norm += Math.abs(r2.getValue());
            }
            top30norms.put(r1.getKey(),cos_norm);
        }

        /* compute the fluctuation over user v */
        LinkedHashMap<Long,LinkedHashMap<Long,Double>> fluctuation = new LinkedHashMap<>();
        for (Map.Entry<Long,LinkedHashMap<Long,Double>> r1 : top30cosines.entrySet()) {
            LinkedHashMap<Long,Double> fluct = new LinkedHashMap<>();
            for (Map.Entry<Long,Double> r2 : r1.getValue().entrySet()) {
                Long2DoubleOpenHashMap v_ratings = getUserRatingVector(r2.getKey());
                double v_mean = Vectors.mean(v_ratings);

                for (Map.Entry<Long,Double> val : v_ratings.entrySet()) {
                    if (val.getKey().equals(r1.getKey())) {
                        double fluct_val = val.getValue() - v_mean;
                        fluct.put(r2.getKey(),fluct_val);
                    }
                }

            }
            fluctuation.put(r1.getKey(), fluct);
        }


        /* compute summed weighted varies (numerator) for each item */
        List<Result> iScore = new ArrayList<>();
        for (Map.Entry<Long,LinkedHashMap<Long,Double>> r1 : top30cosines.entrySet()) {
            LinkedHashMap<Long,Double> varies = new LinkedHashMap<>();
            double summed_varies = 0.0;
            for (Map.Entry<Long,Double> r2 : r1.getValue().entrySet()) {
                varies.put(r2.getKey(), r2.getValue() * fluctuation.get(r1.getKey()).get(r2.getKey()));
            }

            for (Map.Entry<Long,Double> r3 : varies.entrySet()) {
                summed_varies += r3.getValue();
            }

            final long iid = r1.getKey();
            final double rating = user_mean + summed_varies / top30norms.get(r1.getKey());
            Result ires = new Result() {
                @Override
                public long getId() {
                    return iid;
                }

                @Override
                public double getScore() {
                    return rating;
                }

                @Override
                public boolean hasScore() {
                    return false;
                }

                @Nullable
                @Override
                public <T extends Result> T as(@Nonnull Class<T> type) {
                    return null;
                }
            };
            iScore.add(ires);
        }
        ResultMap scores = Results.newResultMap(iScore);

        return scores;

//        throw new UnsupportedOperationException("not yet implemented");
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

}
