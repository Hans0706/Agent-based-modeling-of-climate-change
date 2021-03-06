package models.trading;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.Random;

public class CaFirm extends Agent<TradingModel.Globals> {
    @Variable
    public double price;
    @Variable
    public double mcost;
    public double RD;
    @Variable
    public double IN;
    public double IM;
    public double v;        //fraction of R&D
    @Variable
    public double thetaIN;
    public double thetaIM;
    public double m1;
    @Variable
    public double EIA;
    @Variable
    public double EIB;
    @Variable
    public double EIG;
    public double B;
    @Variable
    public double w;
    public double enPrice;
    public double carbon_emission;

    @Override
    public void init() {
//        EIG=EIG*(1-getPrng().beta(1,3).sample());
//        EIA=EIA*(1-getPrng().beta(1,3).sample());
//        EIB=EIB*(1-getPrng().beta(1,3).sample());
        EIG = 100;
        EIA = 100;
        EIB = 100;
        IN = 2000;
        IM = 20000;
        w = 2000;
        carbon_emission=1500;
        B = 15;
        m1 = 0.04;

    }

    public static Action<CaFirm> receiveWage =
            Action.create(CaFirm.class, caFirm -> {
                caFirm.getMessagesOfType(Messages.WageChange.class).forEach(mes -> {
                    caFirm.w = mes.wage;
                });
            });

    public static Action<CaFirm> sendEIChange =
            Action.create(CaFirm.class, caFirm -> {
                Random r = new Random();
                double t1 = r.nextDouble() - 0.5;
                double t2 = 0.3;

                caFirm.thetaIN = 1 - Math.exp(-t1 * (caFirm.IN / caFirm.w));
                caFirm.thetaIM = 1 - Math.exp(-t2 * (caFirm.IM / caFirm.w));
                if (caFirm.thetaIN > 0) {
                    caFirm.EIG = caFirm.EIG * (1 - caFirm.getPrng().beta(1, 3).sample());
                    caFirm.EIA = caFirm.EIA * (1 - caFirm.getPrng().beta(1, 3).sample());
                    caFirm.EIB = caFirm.EIB * (1 - caFirm.getPrng().beta(1, 3).sample());
                }
                caFirm.getLinks(Links.CaFtoCoF.class).send(Messages.EIChange.class, (msg, link) -> {
                    msg.EIA = caFirm.EIA;
                    msg.EIG = caFirm.EIG;
                });

                caFirm.getLinks(Links.CaFtoLabor.class).send(Messages.EIChange.class, (msg, link) -> {
                    msg.EIG = caFirm.EIG;
                });
            });

    public static Action<CaFirm> receiveEnergyPrices =
            Action.create(CaFirm.class, caFirm -> {
                caFirm.getMessagesOfType(Messages.SendenergyPrice.class).forEach(mes -> {
                    caFirm.enPrice = mes.price;
                });
                caFirm.mcost = caFirm.w / caFirm.B + caFirm.EIG * caFirm.enPrice * 1000;
                caFirm.price = (1 + caFirm.m1) * caFirm.mcost;

            });

    public static Action<CaFirm> sendTax =
            Action.create(CaFirm.class, caFirm -> {
                caFirm
                        .getLinks(Links.CaFtoGovern.class)
                        .send(Messages.sendTax.class, (msg, link) -> {
                            msg.CaF_tax = (caFirm.price - caFirm.mcost) * caFirm.getGlobals().c_tax;
                        });
            });

    public static Action<CaFirm> sendEmission =
            Action.create(CaFirm.class, caFirm -> {
                Random r = new Random();
                if(caFirm.getGlobals().mode==2){
                    caFirm.carbon_emission+=((caFirm.price-caFirm.mcost)/caFirm.mcost)*caFirm.carbon_emission+(1 - 2 * r.nextDouble())*0.04*caFirm.carbon_emission;

                }
                else if(caFirm.getGlobals().mode==1){
                    caFirm.carbon_emission+=((caFirm.price-caFirm.mcost)/caFirm.mcost)*caFirm.carbon_emission-(1 - 2 * r.nextDouble())*caFirm.carbon_emission;

                }
                else{
                    if(caFirm.getGlobals().year_num<30){
                        caFirm.carbon_emission+=((caFirm.price-caFirm.mcost)/caFirm.mcost)*caFirm.carbon_emission+(1 - 2 * r.nextDouble())*0.04*caFirm.carbon_emission;

                    }
                    else{
                        caFirm.carbon_emission+=((caFirm.price-caFirm.mcost)/caFirm.mcost)*caFirm.carbon_emission-(1 - 2 * r.nextDouble())*caFirm.carbon_emission;

                    }

                }

                caFirm
                        .getLinks(Links.CaFtoGovern.class)
                                .send(Messages.sendEmissions.class, (msg, link) -> {
                            msg.emissions = caFirm.carbon_emission;
                        });
            });

    public static Action<CaFirm> receiveLP =
            Action.create(CaFirm.class, caFirm -> {


                caFirm.getMessagesOfType(Messages.LPChange.class).forEach(mes -> {

                    caFirm.B = mes.averageLP2;

                });


            });

}
