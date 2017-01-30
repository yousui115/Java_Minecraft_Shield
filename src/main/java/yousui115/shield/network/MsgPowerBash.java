package yousui115.shield.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MsgPowerBash implements IMessage
{
    private double range;
    private int power;
    private int amount;
    private int entityId;

    /**
     * ■コンストラクタ(必須！)
     */
    public MsgPowerBash(){}

    /**
     * ■コンストラクタ
     */
    public MsgPowerBash(double rangeIn, int powerIn, int amountIn, int id)
    {
        this.range = rangeIn;
        this.power = powerIn;
        this.amount = amountIn;
        this.entityId = id;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.range = buf.readDouble();
        this.power = buf.readInt();
        this.amount = buf.readInt();
        this.entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeDouble(this.range);
        buf.writeInt(this.power);
        buf.writeInt(this.amount);
        buf.writeInt(this.entityId);
    }

    public double getRange() { return this.range; }
    public int getPower() { return this.power; }
    public int getAmount() { return this.amount; }
    public int getEntityID() { return this.entityId; }

}
