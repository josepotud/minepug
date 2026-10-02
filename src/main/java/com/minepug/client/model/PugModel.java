package com.minepug.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.util.Mth;

/**
 * Modelo del carlino para 26.3.
 * <p>
 * Geometría portada del modelo de pug de <b>MorePugMod</b> (pugpoggg,
 * licencia MIT): cabeza grande y achatada con orejas caídas, cuerpo
 * rechoncho, patas cortas y cola rizada de tres tramos.
 */
public class PugModel extends EntityModel<WolfRenderState> {
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart mane;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart tail;

	public PugModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.mane = root.getChild("mane");
		this.rightHindLeg = root.getChild("right_hind_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftFrontLeg = root.getChild("left_front_leg");
		this.tail = root.getChild("tail");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Cabeza grande (7x7x7) con orejas caídas y hocico.
		// El pivote está en el cuello (borde inferior-trasero de la cabeza)
		// para que al girar la cabeza lo haga de forma natural.
		PartDefinition head = root.addOrReplaceChild(
				"head", CubeListBuilder.create(), PartPose.offset(0.5F, 10.5F, -4.5F)
		);
		head.addOrReplaceChild(
				"head_main",
				CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, -1.0F, -6.5F, 7.0F, 7.0F, 7.0F),
				PartPose.ZERO
		);
		head.addOrReplaceChild(
				"left_ear",
				CubeListBuilder.create().texOffs(0, 34).addBox(2.75F, -0.75F, -4.75F, 1.0F, 4.0F, 4.0F),
				PartPose.ZERO
		);
		head.addOrReplaceChild(
				"right_ear",
				CubeListBuilder.create().texOffs(33, 20).addBox(-4.75F, -0.75F, -4.75F, 1.0F, 4.0F, 4.0F),
				PartPose.ZERO
		);
		head.addOrReplaceChild(
				"muzzle_top",
				CubeListBuilder.create().texOffs(0, 7).addBox(-3.0F, 2.0F, -7.5F, 5.0F, 4.0F, 1.0F),
				PartPose.ZERO
		);

		// Cuerpo rechoncho (8x7x13, rotado como en el modelo original).
		PartDefinition body = root.addOrReplaceChild(
				"body", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, (float) (Math.PI / 2.0), 0.0F, 0.0F)
		);
		PartDefinition bodyRotation = body.addOrReplaceChild(
				"body_rotation", CubeListBuilder.create(), PartPose.ZERO
		);
		bodyRotation.addOrReplaceChild(
				"body_sub_1",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -1.0F, -8.0F, 8.0F, 7.0F, 13.0F),
				PartPose.offsetAndRotation(0.0F, -3.0F, -5.0F, (float) (Math.PI / 2.0), 0.0F, 0.0F)
		);

		// Pliegue del pecho (el "mane" del modelo original).
		PartDefinition mane = root.addOrReplaceChild(
				"mane", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0F, 14.0F, -3.0F, (float) (Math.PI / 2.0), 0.0F, 0.0F)
		);
		PartDefinition maneRotation = mane.addOrReplaceChild(
				"mane_rotation", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0F, 2.5F, -2.5F, (float) (Math.PI / 2.0), 0.0F, 0.0F)
		);
		maneRotation.addOrReplaceChild(
				"mane_sub_1",
				CubeListBuilder.create().texOffs(21, 0).addBox(-1.0F, -5.5F, 1.5F, 2.0F, 6.0F, 1.0F),
				PartPose.ZERO
		);

		// Patas cortas y gruesas.
		root.addOrReplaceChild(
				"left_hind_leg",
				CubeListBuilder.create().texOffs(0, 0).addBox(0.25F, 4.0F, -3.5F, 3.0F, 4.0F, 3.0F),
				PartPose.offset(0.5F, 16.0F, 7.0F)
		);
		root.addOrReplaceChild(
				"right_hind_leg",
				CubeListBuilder.create().texOffs(21, 20).addBox(-1.25F, 4.0F, -3.5F, 3.0F, 4.0F, 3.0F),
				PartPose.offset(-2.5F, 16.0F, 7.0F)
		);
		root.addOrReplaceChild(
				"left_front_leg",
				CubeListBuilder.create().texOffs(29, 0).addBox(0.25F, 4.0F, -1.5F, 3.0F, 4.0F, 3.0F),
				PartPose.offset(0.5F, 16.0F, -4.0F)
		);
		root.addOrReplaceChild(
				"right_front_leg",
				CubeListBuilder.create().texOffs(28, 28).addBox(-1.25F, 4.0F, -1.5F, 3.0F, 4.0F, 3.0F),
				PartPose.offset(-2.5F, 16.0F, -4.0F)
		);

		// Cola rizada en tres tramos.
		root.addOrReplaceChild(
				"tail",
				CubeListBuilder.create()
						.texOffs(0, 20).addBox(0.0F, 0.0F, -1.5F, 2.0F, 3.0F, 1.0F)
						.texOffs(29, 7).addBox(0.0F, -0.25F, -4.0F, 1.0F, 1.0F, 3.0F)
						.texOffs(0, 24).addBox(-1.0165F, 0.0143F, -3.2622F, 1.0F, 1.0F, 2.0F),
				PartPose.offset(-1.0F, 12.0F, 8.0F)
		);

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(WolfRenderState state) {
		super.setupAnim(state);

		float animationPos = state.walkAnimationPos;
		float animationSpeed = state.walkAnimationSpeed;

		// Meneo de la cola (el carlino nunca está enfadado).
		this.tail.yRot = Mth.cos(animationPos * 0.6662F) * 1.4F * animationSpeed;

		if (state.isSitting) {
			this.setSittingPose(state);
		} else {
			this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662F) * 1.4F * animationSpeed;
			this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * 1.4F * animationSpeed;
			this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * 1.4F * animationSpeed;
			this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662F) * 1.4F * animationSpeed;
		}

		// Sacudida al salir del agua.
		this.body.zRot = state.getBodyRollAngle(-0.16F);

		// Cabeza: sigue la mirada de la entidad e inclina la cabeza por curiosidad.
		this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
		this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
		this.head.zRot = state.headRollAngle + state.getBodyRollAngle(0.0F);

		this.tail.xRot = state.tailAngle;
	}

	/**
	 * Pose sentada portada del modelo original (deltas sobre la pose de pie,
	 * escaladas por la edad, igual que hace el modelo del lobo vanilla).
	 */
	private void setSittingPose(WolfRenderState state) {
		float ageScale = state.ageScale;

		this.body.y += 4.0F * ageScale;
		this.body.z -= 2.0F * ageScale;
		this.body.xRot = (float) (Math.PI / 4.0);

		this.mane.y += 2.0F * ageScale;
		this.mane.xRot = 1.2566371F;

		this.tail.y += 9.0F * ageScale;
		this.tail.z -= 2.0F * ageScale;

		this.rightHindLeg.y += 6.7F * ageScale;
		this.rightHindLeg.z -= 5.0F * ageScale;
		this.rightHindLeg.xRot = (float) (Math.PI * 3.0 / 2.0);
		this.leftHindLeg.y += 6.7F * ageScale;
		this.leftHindLeg.z -= 5.0F * ageScale;
		this.leftHindLeg.xRot = (float) (Math.PI * 3.0 / 2.0);

		this.rightFrontLeg.xRot = 5.811947F;
		this.rightFrontLeg.x += 0.01F * ageScale;
		this.rightFrontLeg.y += 1.0F * ageScale;
		this.leftFrontLeg.xRot = 5.811947F;
		this.leftFrontLeg.x += 0.01F * ageScale;
		this.leftFrontLeg.y += 1.0F * ageScale;
	}
}
