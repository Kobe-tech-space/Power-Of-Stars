package com.example.tutorial.recipe;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ModSettings;
import com.example.tutorial.event.AstralEquipment;
import com.example.tutorial.item.ModItems;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.*;
/** Actual blasting recipe, also usable by hoppers. Never returns more than 1/4 of input metal. */
public final class AstralRecyclingRecipe extends BlastingRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,TutorialMod.MOD_ID);
    public static final RegistryObject<RecipeSerializer<AstralRecyclingRecipe>> SERIALIZER=SERIALIZERS.register("astral_recycling",Serializer::new);
    private final Ingredient accepted;
    private final int maximum;
    public AstralRecyclingRecipe(ResourceLocation id,Ingredient input,int maximum) {
        super(id,"astral_recycling",CookingBookCategory.MISC,input,new ItemStack(ModItems.STAR_INGOT.get(),maximum),0,200);
        this.accepted=input;this.maximum=maximum;
    }
    public int count(ItemStack s) {
        if(!ModSettings.enabled("recycling.enabled") || !accepted.test(s) || s.isEnchanted() || !s.isDamageableItem()) return 0;
        return Math.max(0,Math.min(maximum,(int)Math.ceil(maximum*(1-(double)s.getDamageValue()/s.getMaxDamage()))));
    }
    @Override public boolean matches(Container c,Level l) {return count(c.getItem(0))>0;}
    @Override public ItemStack assemble(Container c,RegistryAccess access) {int n=count(c.getItem(0));return n==0?ItemStack.EMPTY:new ItemStack(ModItems.STAR_INGOT.get(),n);}
    @Override public RecipeSerializer<?> getSerializer() {return SERIALIZER.get();}
    public static final class Serializer implements RecipeSerializer<AstralRecyclingRecipe> {
        @Override public AstralRecyclingRecipe fromJson(ResourceLocation id,JsonObject json) {return new AstralRecyclingRecipe(id,Ingredient.fromJson(json.get("ingredient")),Math.max(1,Math.min(2,json.get("maximum").getAsInt())));}
        @Override public AstralRecyclingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b) {return new AstralRecyclingRecipe(id,Ingredient.fromNetwork(b),b.readVarInt());}
        @Override public void toNetwork(FriendlyByteBuf b,AstralRecyclingRecipe r) {r.accepted.toNetwork(b);b.writeVarInt(r.maximum);}
    }
}
