package com.example.gulp;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class BellyModel {
   public static final int BELLY_TOP = 5;
   public static final int BELLY_HEIGHT = 6;

   public static LayerDefinition createLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.m_171576_();
      root.m_171599_("belly_full", CubeListBuilder.m_171558_().m_171514_(16, 16).m_171488_(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.02F)), PartPose.f_171404_);
      root.m_171599_("jacket_full", CubeListBuilder.m_171558_().m_171514_(16, 32).m_171488_(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.27F)), PartPose.f_171404_);
      root.m_171599_("belly_low", CubeListBuilder.m_171558_().m_171514_(16, 21).m_171488_(-4.0F, 5.0F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.02F)), PartPose.f_171404_);
      root.m_171599_("jacket_low", CubeListBuilder.m_171558_().m_171514_(16, 37).m_171488_(-4.0F, 5.0F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.27F)), PartPose.f_171404_);
      root.m_171599_("belly_full_clothing", CubeListBuilder.m_171558_().m_171514_(16, 16).m_171488_(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.09F)), PartPose.f_171404_);
      root.m_171599_("jacket_full_clothing", CubeListBuilder.m_171558_().m_171514_(16, 32).m_171488_(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.33F)), PartPose.f_171404_);
      root.m_171599_("belly_low_clothing", CubeListBuilder.m_171558_().m_171514_(16, 21).m_171488_(-4.0F, 5.0F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.09F)), PartPose.f_171404_);
      root.m_171599_("jacket_low_clothing", CubeListBuilder.m_171558_().m_171514_(16, 37).m_171488_(-4.0F, 5.0F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.33F)), PartPose.f_171404_);
      return LayerDefinition.m_171565_(mesh, 64, 64);
   }
}
