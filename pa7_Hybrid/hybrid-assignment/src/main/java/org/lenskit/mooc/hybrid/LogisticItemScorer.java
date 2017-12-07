package org.lenskit.mooc.hybrid;

import it.unimi.dsi.fastutil.longs.LongSet;
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

        RealVector coeff = logisticModel.getCoefficients();
        ArrayList<Double> ctr_scores = new ArrayList<>();

        double expo = logisticModel.getIntercept();

        for (long item : items) {
            double b_ui = biasModel.getIntercept() + biasModel.getItemBias(item) + biasModel.getUserBias(user);
            expo += b_ui * coeff.getEntry(0) + Math.log10(ratingSummary.getItemRatingCount(item)) * coeff.getEntry(1);

            for (ItemScorer is : recommenders.getItemScorers()) {
                ctr_scores.add(is.score(user, item).getScore() - b_ui);
            }

            for (int i = 2; i < coeff.getDimension(); ++i) {
                expo += coeff.getEntry(i) * ctr_scores.get(i - 1);
            }

            double prob = 1 / (1 + Math.exp(-expo));
            results.add(Results.create(item, prob));
        }

        return Results.newResultMap(results);

//        throw new UnsupportedOperationException("item scorer not implemented");
    }
}
