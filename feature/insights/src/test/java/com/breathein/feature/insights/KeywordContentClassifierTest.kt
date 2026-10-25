package com.breathein.feature.insights

import com.breathein.core.model.ContentCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class KeywordContentClassifierTest {

    @Test
    fun matchesHashtagsAndWords() {
        assertEquals(ContentCategory.Fitness, KeywordContentClassifier.categoryOf("mike · Leg day finisher #legday #gym"))
        assertEquals(ContentCategory.Cooking, KeywordContentClassifier.categoryOf("Easy 15 minute pasta recipe"))
    }

    @Test
    fun splitsCloseCategories() {
        assertEquals(ContentCategory.Guitar, KeywordContentClassifier.categoryOf("fingerstyle guitar cover"))
        assertEquals(ContentCategory.Dance, KeywordContentClassifier.categoryOf("new dance challenge #choreography"))
        assertEquals(ContentCategory.StandUp, KeywordContentClassifier.categoryOf("crowdwork gone wrong #standupcomedy"))
        assertEquals(ContentCategory.Cats, KeywordContentClassifier.categoryOf("my cat stealing food #catsofinstagram"))
        assertEquals(ContentCategory.Babies, KeywordContentClassifier.categoryOf("toddler tries lemon for the first time"))
    }

    @Test
    fun noMatch_isOther() {
        assertEquals(ContentCategory.Other, KeywordContentClassifier.categoryOf("just vibes"))
    }
}
