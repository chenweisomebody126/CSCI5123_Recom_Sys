package org.lenskit.mooc.nonpers.mean;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.baseline.MeanDamping;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.ratings.Rating;
import org.lenskit.inject.Transient;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider class that builds the mean rating item scorer, computing damped item means from the
 * ratings in the DAO.
 */
public class DampedItemMeanModelProvider implements Provider<ItemMeanModel> {
    /**
     * A logger that you can use to emit debug messages.
     */
    private static final Logger logger = LoggerFactory.getLogger(DampedItemMeanModelProvider.class);

    /**
     * The data access object, to be used when computing the mean ratings.
     */
    private final DataAccessObject dao;
    /**
     * The damping factor.
     */
    private final double damping;

    /**
     * Constructor for the mean item score provider.
     *
     * <p>The {@code @Inject} annotation tells LensKit to use this constructor.
     *
     * @param dao The data access object (DAO), where the builder will get ratings.  The {@code @Transient}
     *            annotation on this parameter means that the DAO will be used to build the model, but the
     *            model will <strong>not</strong> retain a reference to the DAO.  This is standard procedure
     *            for LensKit models.
     * @param damping The damping factor for Bayesian damping.  This is number of fake global-mean ratings to
     *                assume.  It is provided as a parameter so that it can be reconfigured.  See the file
     *                {@code damped-mean.groovy} for how it is used.
     */
    @Inject
    public DampedItemMeanModelProvider(@Transient DataAccessObject dao,
                                       @MeanDamping double damping) {
        this.dao = dao;
        this.damping = damping;
    }

    /**
     * Construct an item mean model.
     *
     * <p>The {@link Provider#get()} method constructs whatever object the provider class is intended to build.</p>
     *
     * @return The item mean model with mean ratings for all items.
     */
    @Override
    public ItemMeanModel get() {
        // TODO Compute damped means
        double globalSum = 0;
        int globalCount = 0;
        // TODO Set up data structures for computing means
        Map<Long, List<Double>> map = new HashMap<>();

        try (ObjectStream<Rating> ratings = dao.query(Rating.class).stream()) {
            for (Rating r : ratings) {
                // this loop will run once for each rating in the data set
                // TODO process this rating
                if (!map.containsKey(r.getItemId())) {
                    map.put(r.getItemId(), new ArrayList<Double>());
                }
                map.get(r.getItemId()).add(r.getValue());
                globalSum += r.getValue();
                globalCount += 1;
            }
        } catch (Exception e) {
            logger.info("error in query dao and loop through it");
//            throw UnsupportedOperationException e;
        }
        logger.info("get global sum {}", globalSum);


        double globalMeanRating = globalSum / globalCount;

        Long2DoubleOpenHashMap dampedMeans = new Long2DoubleOpenHashMap();
        // TODO Finalize means to store them in the mean model
        for (Long itemId : map.keySet()) {
            double sum = 0;
            for (Double rating : map.get(itemId)) {
                sum += rating;
            }
            double avg = (sum + this.damping * globalMeanRating) / (map.get(itemId).size() + this.damping);
            dampedMeans.put(itemId, (Double) avg);
        }
        logger.info("get means {}", dampedMeans.get(2959));
        logger.info("get means {}", dampedMeans.get(1203));
        logger.info("computed mean ratings for {} items", dampedMeans.size());

        return new ItemMeanModel(dampedMeans);
    }
}
