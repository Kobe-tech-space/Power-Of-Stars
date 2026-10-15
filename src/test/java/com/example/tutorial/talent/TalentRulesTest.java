package com.example.tutorial.talent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TalentRulesTest {
    @Test void experienceThresholdsAndCap(){
        assertEquals(0,TalentRules.earned(-10)); assertEquals(0,TalentRules.earned(99));
        assertEquals(1,TalentRules.earned(100)); assertEquals(1,TalentRules.earned(224));
        assertEquals(2,TalentRules.earned(225)); assertEquals(24,TalentRules.earned(Integer.MAX_VALUE));
        assertEquals(0,TalentRules.progress(225)); assertEquals(124,TalentRules.progress(224));
    }
    @Test void invalidPacketsAndPrerequisites(){
        int[] r=new int[12]; assertFalse(TalentRules.canLearn(100,r,-1));
        assertFalse(TalentRules.canLearn(100,r,12)); assertFalse(TalentRules.canLearn(100,r,1));
        assertFalse(TalentRules.canLearn(0,r,0)); assertTrue(TalentRules.canLearn(100,r,0));
        r[0]=2; assertFalse(TalentRules.canLearn(225,r,1)); assertTrue(TalentRules.canLearn(375,r,1));
        r[0]=3; assertFalse(TalentRules.canLearn(10000,r,0));
    }
    @Test void sanitizeAndPointBudget(){
        int[] r=TalentRules.sanitize(new int[]{-1,3,3,3});assertArrayEquals(new int[12],r);
        int[] excessive=new int[12];java.util.Arrays.fill(excessive,999);
        assertEquals(24,TalentRules.spent(excessive));assertEquals(3,TalentRules.sanitize(excessive)[0]);
        assertFalse(TalentRules.canLearn(Integer.MAX_VALUE,excessive,8));
    }
    @Test void resetRefundIsDerivedNotDuplicated(){
        int exp=1000;int[] ranks=new int[12];ranks[0]=2;ranks[1]=1;
        int before=TalentRules.earned(exp)-TalentRules.spent(ranks);
        assertEquals(before+3,TalentRules.earned(exp)-TalentRules.spent(new int[12]));
    }
}
