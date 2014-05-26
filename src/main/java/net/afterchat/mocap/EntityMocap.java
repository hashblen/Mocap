/**
 * The Mocap mod is a Minecraft mod aimed at film-makers who wish to
 * record the motion of their character and replay it back via a scripted
 * entity. It allows you to create a whole series of composite characters
 * for use in your own videos without having to splash out on extra accounts
 * or enlist help.
 * 
 * Copyright (C) 2013-2014 Echeb Keso
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.*
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.afterchat.mocap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class EntityMocap extends EntityLiving {

	/**
	 * A list of pending actions the Entity has to perform, injected by the
	 * replay thread.
	 */
	private int itemInUseCount = 0;

	public List<MocapAction> eventsList = Collections
			.synchronizedList(new ArrayList<MocapAction>());

	public EntityMocap(World par1World) {
		super(par1World);
		this.tasks.taskEntries.clear();
		this.targetTasks.taskEntries.clear();
	}

	public void setItemInUseCount(int itemInUseCount) {
		this.itemInUseCount = itemInUseCount;
	}

	public int getItemInUseCount() {
		return itemInUseCount;
	}

	private void replayShootArrow(MocapAction ma) {
		float f = (float) ma.arrowCharge / 20.0F;
		f = (f * f + f * 2.0F) / 3.0F;

		if ((double) f < 0.1D) {
			return;
		}

		if (f > 1.0F) {
			f = 1.0F;
		}

		EntityArrow entityarrow = new EntityArrow(worldObj, this, f * 2.0F);
		entityarrow.canBePickedUp = 1;
		this.playSound("random.bow", 1.0F,
				1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
		worldObj.spawnEntityInWorld(entityarrow);
	}

	private void processActions(MocapAction ma) {
		switch (ma.type) {
		case MocapActionTypes.CHAT: {
			ChatComponentTranslation chatcomponenttranslation1 = new ChatComponentTranslation(
					"chat.type.text", new Object[] { this.func_145748_c_(),
							ma.message });
			MinecraftServer.getServer().getConfigurationManager()
					.sendChatMsgImpl(chatcomponenttranslation1, false);
			break;
		}

		case MocapActionTypes.SWIPE: {
			swingItem();
			break;
		}

		case MocapActionTypes.EQUIP: {
			if (ma.armorId == -1) {
				setCurrentItemOrArmor(ma.armorSlot, null);
			} else {
				ItemStack loadedEquip = ItemStack
						.loadItemStackFromNBT(ma.itemData);
				setCurrentItemOrArmor(ma.armorSlot, loadedEquip);
			}

			break;
		}

		case MocapActionTypes.DROP: {
			ItemStack foo = ItemStack.loadItemStackFromNBT(ma.itemData);
			EntityItem ea = new EntityItem(worldObj, posX, posY
					- 0.30000001192092896D + (double) getEyeHeight(), posZ, foo);
			Random rand = new Random();
			float f = 0.3F;
			ea.motionX = (double) (-MathHelper.sin(rotationYaw / 180.0F
					* (float) Math.PI)
					* MathHelper.cos(rotationPitch / 180.0F * (float) Math.PI) * f);
			ea.motionZ = (double) (MathHelper.cos(rotationYaw / 180.0F
					* (float) Math.PI)
					* MathHelper.cos(rotationPitch / 180.0F * (float) Math.PI) * f);
			ea.motionY = (double) (-MathHelper.sin(rotationPitch / 180.0F
					* (float) Math.PI)
					* f + 0.1F);
			f = 0.02F;
			float f1 = rand.nextFloat() * (float) Math.PI * 2.0F;
			f *= rand.nextFloat();
			ea.motionX += Math.cos((double) f1) * (double) f;
			ea.motionY += (double) ((rand.nextFloat() - rand.nextFloat()) * 0.1F);
			ea.motionZ += Math.sin((double) f1) * (double) f;
			worldObj.spawnEntityInWorld(ea);
			break;
		}

		case MocapActionTypes.SHOOTARROW: {
			replayShootArrow(ma);
			break;
		}

		case MocapActionTypes.PLACEBLOCK: {
			ItemStack foo = ItemStack.loadItemStackFromNBT(ma.itemData);

			if (foo.getItem() instanceof ItemBlock) {
				ItemBlock f = (ItemBlock) foo.getItem();
				f.placeBlockAt(foo, null, worldObj, ma.xCoord, ma.yCoord,
						ma.zCoord, 0, 0, 0, 0, foo.getItemDamage());
			}

			break;
		}
		}
	}

	public void onLivingUpdate() {
		if (eventsList.size() > 0) {
			MocapAction ma = eventsList.remove(0);
			processActions(ma);
		}

		this.updateArmSwingProgress();

		if (this.newPosRotationIncrements > 0) {
			double d0 = this.posX + (this.newPosX - this.posX)
					/ (double) this.newPosRotationIncrements;
			double d1 = this.posY + (this.newPosY - this.posY)
					/ (double) this.newPosRotationIncrements;
			double d2 = this.posZ + (this.newPosZ - this.posZ)
					/ (double) this.newPosRotationIncrements;
			double d3 = MathHelper.wrapAngleTo180_double(this.newRotationYaw
					- (double) this.rotationYaw);
			this.rotationYaw = (float) ((double) this.rotationYaw + d3
					/ (double) this.newPosRotationIncrements);
			this.rotationPitch = (float) ((double) this.rotationPitch + (this.newRotationPitch - (double) this.rotationPitch)
					/ (double) this.newPosRotationIncrements);
			--this.newPosRotationIncrements;
			this.setPosition(d0, d1, d2);
			this.setRotation(this.rotationYaw, this.rotationPitch);
		} else if (!this.isClientWorld()) {
			this.motionX *= 0.98D;
			this.motionY *= 0.98D;
			this.motionZ *= 0.98D;
		}

		if (Math.abs(this.motionX) < 0.005D) {
			this.motionX = 0.0D;
		}

		if (Math.abs(this.motionY) < 0.005D) {
			this.motionY = 0.0D;
		}

		if (Math.abs(this.motionZ) < 0.005D) {
			this.motionZ = 0.0D;
		}

		if (!this.isClientWorld()) {
			this.rotationYawHead = this.rotationYaw;
		}

		this.prevLimbSwingAmount = this.limbSwingAmount;
		double d0 = this.posX - this.prevPosX;
		double d1 = this.posZ - this.prevPosZ;
		float f6 = MathHelper.sqrt_double(d0 * d0 + d1 * d1) * 4.0F;

		if (f6 > 1.0F) {
			f6 = 1.0F;
		}

		this.limbSwingAmount += (f6 - this.limbSwingAmount) * 0.4F;
		this.limbSwing += this.limbSwingAmount;
	}

	protected void entityInit() {
		super.entityInit();
		this.dataWatcher.addObject(20, "ridgedog");
	}

	public void setSkinSource(String par1Str) {
		this.dataWatcher.updateObject(20, par1Str);
	}

	public String getSkinSource() {
		return this.dataWatcher.getWatchableObjectString(20);
	}

	protected boolean isAIEnabled() {
		return false;
	}

}
