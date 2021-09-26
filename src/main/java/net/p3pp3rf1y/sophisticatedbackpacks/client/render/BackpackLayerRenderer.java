package net.p3pp3rf1y.sophisticatedbackpacks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IRenderedBatteryUpgrade;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackRenderInfo;
import net.p3pp3rf1y.sophisticatedbackpacks.client.ClientProxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BackpackLayerRenderer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
	private static final float CHILD_Y_OFFSET = 0.3F;
	private static final float CHILD_Z_OFFSET = 0.1F;
	private static final float CHILD_SCALE = 0.55F;
	private static final Map<EntityType<?>, Vec3> entityTranslations;

	static {
		entityTranslations = new HashMap<>();
		entityTranslations.put(EntityType.ENDERMAN, new Vec3(0, -0.8, 0));
	}

	private final BackpackModel model;

	public BackpackLayerRenderer(RenderLayerParent<T, M> entityRendererIn) {
		super(entityRendererIn);
		EntityModelSet entityModels = Minecraft.getInstance().getEntityModels();
		model = new BackpackModel(entityModels.bakeLayer(ClientProxy.BACKPACK_LAYER));
	}

	@Override
	public void render(PoseStack matrixStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity instanceof AbstractClientPlayer player) {
			SophisticatedBackpacks.PROXY.getPlayerInventoryProvider().getBackpackFromRendered(player).ifPresent(backpackRenderInfo -> {
				matrixStack.pushPose();
				boolean wearsArmor = !backpackRenderInfo.isArmorSlot() && !player.getInventory().armor.get(EquipmentSlot.CHEST.getIndex()).isEmpty();
				ItemStack backpack = backpackRenderInfo.getBackpack();
				renderBackpack(player, matrixStack, buffer, packedLight, backpack, wearsArmor, model);
				matrixStack.popPose();
			});
		} else {
			ItemStack chestStack = entity.getItemBySlot(EquipmentSlot.CHEST);
			if (chestStack.getItem() instanceof BackpackItem) {
				renderBackpack(entity, matrixStack, buffer, packedLight, chestStack, false, model);
			}
		}
	}

	public static void renderBackpack(LivingEntity livingEntity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, ItemStack backpack, boolean wearsArmor, BackpackModel model) {
		if (livingEntity.isCrouching()) {
			matrixStack.translate(0D, 0.2D, 0D);
			matrixStack.mulPose(Vector3f.XP.rotationDegrees(90F / (float) Math.PI));
		}

		matrixStack.mulPose(Vector3f.YP.rotationDegrees(180));
		float zOffset = wearsArmor ? -0.35f : -0.3f;
		float yOffset = -0.75f;

		if (livingEntity.isBaby()) {
			zOffset += CHILD_Z_OFFSET;
			yOffset = CHILD_Y_OFFSET;
		}

		matrixStack.translate(0, yOffset, zOffset);

		if (livingEntity.isBaby()) {
			matrixStack.scale(CHILD_SCALE, CHILD_SCALE, CHILD_SCALE);
		}

		if (entityTranslations.containsKey(livingEntity.getType())) {
			Vec3 translVector = entityTranslations.get(livingEntity.getType());
			matrixStack.translate(translVector.x(), translVector.y(), translVector.z());
		}

		backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).ifPresent(wrapper -> {
			int clothColor = wrapper.getClothColor();
			int borderColor = wrapper.getBorderColor();

			BackpackRenderInfo renderInfo = wrapper.getRenderInfo();

			Optional<IRenderedBatteryUpgrade.BatteryRenderInfo> batteryRenderInfo = renderInfo.getBatteryRenderInfo();
			model.render(matrixStack, buffer, packedLight, clothColor, borderColor, backpack.getItem(), wrapper.getRenderInfo());
		});
	}
}