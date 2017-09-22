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
public class ThresholdUserProfileBuilder implements UserProfileBuilder {
    /**
     * The lowest rating that will be considered in the user's profile.
     */
    private static final double RATING_THRESHOLD = 3.5;

    /**
     * The tag model, to get item tag vectors.
     */
    private final TFIDFModel model;

    @Inject
    public ThresholdUserProfileBuilder(TFIDFModel m) {
        model = m;
    }

    @Override
    public Map<String, Double> makeUserProfile(@Nonnull List<Rating> ratings) {
        // Create a new vector over tags to accumulate the user profile
        Map<String,Double> profile = new HashMap<>();

        // Iterate over the user's ratings to build their profile
        for (Rating r: ratings) {
            if (r.getValue() >= RATING_THRESHOLD) {

                // TODO Get this item's vector and add it to the user's profile
                Long itemId = r.getItemId();
                Map<String, Double> itemVector = this.model.getItemVector(itemId);
                for (Map.Entry<String, Double> tag_vec : itemVector.entrySet()){
                    String tag = tag_vec.getKey();
                    if (!profile.containsKey(tag)){
                        profile.put(tag, 0.);
                    }
                    profile.put(tag, profile.get(tag) + tag_vec.getValue());
                }

            }
        }

        // The profile is accumulated, return it.
        return profile;
    }
}
