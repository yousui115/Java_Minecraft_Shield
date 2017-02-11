package yousui115.shield.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MsgPowerBash implements IMessage
{
    private float tick;
    private int rangeNum;
    private double range[];
    private float offsetYaw;
    private int power;
    private int amount;

    /**
     * ■コンストラクタ(必須！)
     */
    public MsgPowerBash(){}

    /**
     * ■コンストラクタ
     */
    public MsgPowerBash(float tickIn, double rangeIn[], float offsetYawIn, int powerIn, int amountIn)
    {
        this.tick = tickIn;
        this.rangeNum = rangeIn.length;
        this.range = new double[this.rangeNum];
        for (int idx = 0; idx < this.rangeNum; idx++)
        {
            this.range[idx] = rangeIn[idx];
        }
        this.offsetYaw = offsetYawIn;
        this.power = powerIn;
        this.amount = amountIn;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.tick = buf.readFloat();
        this.rangeNum = buf.readInt();
        this.range = new double[this.rangeNum];
        for (int idx = 0; idx < this.rangeNum; idx++)
        {
            this.range[idx] = buf.readDouble();
        }
        this.offsetYaw = buf.readFloat();
        this.power = buf.readInt();
        this.amount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeFloat(this.tick);
        buf.writeInt(this.rangeNum);
        for (int idx = 0; idx < this.range.length; idx++)
        {
            buf.writeDouble(range[idx]);
        }
        buf.writeFloat(this.offsetYaw);
        buf.writeInt(this.power);
        buf.writeInt(this.amount);
    }

    public float    getTick()  { return this.tick; }
    public int      getRangeNum() { return this.rangeNum; }
    public double[] getRange() { return this.range; }
    public float    getOffsetYaw() { return this.offsetYaw; }
    public int      getPower() { return this.power; }
    public int      getAmount() { return this.amount; }
}
