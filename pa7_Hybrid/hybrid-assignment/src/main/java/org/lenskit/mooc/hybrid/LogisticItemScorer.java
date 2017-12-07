package org.lenskit.mooc.hybrid;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.bias.BiasModel;
import org.lenskit.bias.UserBiasModel;
import org.lenskit.data.ratings.RatingSummary;
import org.lenskit.results.Results;
import org.lenskit.util.collections.LongUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Item scorer that does a logistic blend of a subsidiary item scorer and popularity.  It tries to predict
 * whether a user has rated a particular item.
 */
public class LogisticItemScorer extends AbstractItemScorer {
    private final LogisticModel logisticModel;
    private final BiasModel biasModel;
    private final RecommenderList recommenders;
    private final RatingSummary ratingSummary;
    private static final Logger logger = LoggerFactory.getLogger(LogisticModelProvider.class);

    @Inject
    public LogisticItemScorer(LogisticModel model, UserBiasModel bias, RecommenderList recs, RatingSummary rs) {
        logisticModel = model;
        biasModel = bias;
        recommenders = recs;
        ratingSummary = rs;
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        // TODO Implement item scorer
        List<Result> results = new ArrayList<>();

        //RealVector coeff = logisticModel.getCoefficients();
        //ArrayList<Double> ctr_scores = new ArrayList<>();

        //double expo = logisticModel.getIntercept();

        int parameterCount = 1 + recommenders.getRecommenderCount() + 1;
        List<ItemScorer> scorers = recommenders.getItemScorers();

        for (long item : items) {
            double b_ui = biasModel.getIntercept() + biasModel.getItemBias(item) + biasModel.getUserBias(user);
            double lg_popularity = Math.log10(ratingSummary.getItemRatingCount(item));
            RealVector x_array = new ArrayRealVector(parameterCount);

            x_array.setEntry(0, b_ui);
            x_array.setEntry(1, lg_popularity);
            int i=2;
            for (ItemScorer scorer: scorers){
                //logger.info("{} before pivot point {}", i, b_ui);
                Result score_result = scorer.score(user, item);
                if (score_result == null) {
                    x_array.setEntry(i, 0.);
                    i+=1;
                    continue;
                }
                double x_value = score_result.getScore() - b_ui;
                //logger.info("{} pivot point", i);
                x_array.setEntry(i, x_value);
                i+=1;
            }
            double y =1.;
            double sigmoid = logisticModel.evaluate(y, x_array);
            //logger.info("sigmoid {} ", sigmoid);
//
//            expo += b_ui * coeff.getEntry(0) + Math.log10(ratingSummary.getItemRatingCount(item)) * coeff.getEntry(1);
//
//            for (ItemScorer is : recommenders.getItemScorers()) {
//                ctr_scores.add(is.score(user, item).getScore() - b_ui);
//            }
//
//            for (int i = 2; i < coeff.getDimension(); ++i) {
//                expo += coeff.getEntry(i) * ctr_scores.get(i - 1);
//            }
//
//            double prob = 1 / (1 + Math.exp(-expo));
            results.add(Results.create(item, sigmoid));
        }

        return Results.newResultMap(results);

//        throw new UnsupportedOperationException("item scorer not implemented");
    }
}
