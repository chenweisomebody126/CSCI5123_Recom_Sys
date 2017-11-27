package org.lenskit.mooc;

import groovy.util.MapEntry;
import org.lenskit.LenskitRecommender;
import org.lenskit.api.Recommender;
import org.lenskit.api.Result;
import org.lenskit.api.ResultList;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.Entity;
import org.lenskit.eval.traintest.AlgorithmInstance;
import org.lenskit.eval.traintest.DataSet;
import org.lenskit.eval.traintest.TestUser;
import org.lenskit.eval.traintest.metrics.MetricColumn;
import org.lenskit.eval.traintest.metrics.MetricResult;
import org.lenskit.eval.traintest.metrics.TypedMetricResult;
import org.lenskit.eval.traintest.recommend.TopNMetric;
import org.lenskit.mooc.cbf.TagData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metric that measures how long a TopN list actually is.
 */
public class TagEntropyMetric extends TopNMetric<TagEntropyMetric.Context> {
    private static final Logger logger = LoggerFactory.getLogger(TagEntropyMetric.class);

    /**
     * Construct a new tag entropy metric metric.
     */
    public TagEntropyMetric() {
        super(TagEntropyResult.class, TagEntropyResult.class);
    }

    @Nonnull
    @Override
    public MetricResult measureUser(TestUser user, int expectedSize, ResultList recommendations, Context context) {
        if (recommendations == null || recommendations.isEmpty()) {
            return MetricResult.empty();
            // no results for this user.
        }
        int n = recommendations.size();

        // get tag data from the context so we can use it
        DataAccessObject dao = context.getDAO();
        double entropy = 0.0;

        // TODO Compute the entropy of the movie list
        // You can get a movie's tags with:
        // dao.query(TagData.ITEM_TAG_TYPE).withAttribute(TagData.ITEM_ID, res.getId()).get();
        // Each entity's tag can be retrieved with 'itemTag.get(TagData.TAG)'
        int neighbors_num = Math.min(10, n);
        int iter_count = 0;
        Map<String, Double> posteriors = new HashMap<>();

        for (Result res : recommendations) {
            if (iter_count == neighbors_num) { break; }
            ++iter_count;

            List<Entity> res_tag =  dao.query(TagData.ITEM_TAG_TYPE).withAttribute(TagData.ITEM_ID, res.getId()).get();

            for (Entity tag : res_tag) {
                String tag_name = tag.get(TagData.TAG);

                if (posteriors.containsKey(tag_name)) {
                    posteriors.put(tag_name, posteriors.get(tag_name) + 1.0 / res_tag.size() / neighbors_num);
                }
                else {
                    posteriors.put(tag_name, 1.0 / res_tag.size() / neighbors_num);
                }
            }
        }

        for (Map.Entry<String, Double> posterior : posteriors.entrySet()) {
            entropy += -posterior.getValue() * Math.log(posterior.getValue()) / Math.log(2.0);
        }

        context.addUser(entropy);

        return new TagEntropyResult(entropy);
    }

    @Nullable
    @Override
    public Context createContext(AlgorithmInstance algorithm, DataSet dataSet, Recommender recommender) {
        return new Context((LenskitRecommender) recommender);
    }

    @Nonnull
    @Override
    public MetricResult getAggregateMeasurements(Context context) {
        return new TagEntropyResult(context.getMeanEntropy());
    }

    public static class TagEntropyResult extends TypedMetricResult {
        @MetricColumn("TopN.TagEntropy")
        public final double entropy;

        public TagEntropyResult(double ent) {
            entropy = ent;
        }

    }

    public static class Context {
        private LenskitRecommender recommender;
        private double totalEntropy;
        private int userCount;

        /**
         * Create a new context for evaluating a particular recommender.
         *
         * @param rec The recommender being evaluated.
         */
        public Context(LenskitRecommender rec) {
            recommender = rec;
        }

        /**
         * Get the recommender being evaluated.
         *
         * @return The recommender being evaluated.
         */
        public LenskitRecommender getRecommender() {
            return recommender;
        }

        /**
         * Get the DAO for the current recommender evaluation.
         */
        public DataAccessObject getDAO() {
            return recommender.get(DataAccessObject.class);
        }

        /**
         * Add the entropy for a user to this context.
         *
         * @param entropy The entropy for one user.
         */
        public void addUser(double entropy) {
            totalEntropy += entropy;
            userCount += 1;
        }

        /**
         * Get the average entropy over all users.
         *
         * @return The average entropy over all users.
         */
        public double getMeanEntropy() {
            return totalEntropy / userCount;
        }
    }
}
