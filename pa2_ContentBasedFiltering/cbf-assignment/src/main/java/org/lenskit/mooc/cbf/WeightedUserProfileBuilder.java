package org.lenskit.mooc.cbf;

import org.lenskit.data.ratings.Rating;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Build a user profile from all positive ratings.
 */
public class WeightedUserProfileBuilder implements UserProfileBuilder {
    /**
     * The tag model, to get item tag vectors.
     */
    private final TFIDFModel model;

    @Inject
    public WeightedUserProfileBuilder(TFIDFModel m) {
        model = m;
    }

    @Override
    public Map<String, Double> makeUserProfile(@Nonnull List<Rating> ratings) {
        // Create a new vector over tags to accumulate the user profile
        Map<String,Double> profile = new HashMap<>();

        // TODO Normalize the user's ratings
        // Iterate over the user's ratings to normalize the ratings and build their profile
        if (ratings.isEmpty()){
            return profile;
        }
        Double mu = 0.;
        for (Rating r : ratings) {
            mu += r.getValue();
        }
        mu /= ratings.size();

        // TODO Build the user's weighted profile
        for (Rating r : ratings) {
            Long itemId = r.getItemId();
            Map<String, Double> itemVector = this.model.getItemVector(itemId);
            for (Map.Entry<String, Double> tag_vec : itemVector.entrySet()){
                String tag = tag_vec.getKey();
                if (!profile.containsKey(tag)){
                    profile.put(tag, 0.);
                }
                profile.put(tag, profile.get(tag) + tag_vec.getValue() * (r.getValue() - mu));
            }
        }

        // The profile is accumulated, return it.
        return profile;
    }
}
