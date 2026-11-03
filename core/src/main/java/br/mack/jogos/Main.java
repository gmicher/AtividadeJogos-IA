package br.mack.jogos;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Main extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private Agente npc;
    private Array<Agente> bando;
    private Vector2 mousePos;
    private float wanderAngle = 0;

    public enum Estado {
        WANDER, PURSUIT
    }

    public class Agente {
        public Vector2 posicao;
        public Vector2 velocidade;
        public Vector2 aceleracao;
        public float maxVelocidade;
        public float maxForca;
        public float raio;
        public Estado estado;

        public Agente(float x, float y) {
            posicao = new Vector2(x, y);
            velocidade = new Vector2(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f));
            aceleracao = new Vector2();
            maxVelocidade = 1.5f; // Velocidade máxima reduzida
            maxForca = 0.08f;     // Força de esterçamento mais suave
            raio = 8f;
            estado = Estado.WANDER;
        }

        public void applyForce(Vector2 force) {
            aceleracao.add(force);
        }

        public void update() {
            velocidade.add(aceleracao);
            velocidade.limit(maxVelocidade);
            posicao.add(velocidade);
            aceleracao.set(0, 0); // Reseta a aceleração por frame

            float largura = Gdx.graphics.getWidth();
            float altura = Gdx.graphics.getHeight();
            if (posicao.x < 0) posicao.x = largura;
            if (posicao.x > largura) posicao.x = 0;
            if (posicao.y < 0) posicao.y = altura;
            if (posicao.y > altura) posicao.y = 0;
        }

        public Vector2 seek(Vector2 target) {
            Vector2 desired = new Vector2(target).sub(posicao);
            desired.setLength(maxVelocidade);
            Vector2 steer = new Vector2(desired).sub(velocidade);
            steer.limit(maxForca);
            return steer;
        }

        public Vector2 arrive(Vector2 target) {
            Vector2 desired = new Vector2(target).sub(posicao);
            float d = desired.len();
            desired.setLength(maxVelocidade);
            if (d < 100) {
                float m = MathUtils.map(0, 100, 0, maxVelocidade, d);
                desired.setLength(m);
            }
            Vector2 steer = new Vector2(desired).sub(velocidade);
            steer.limit(maxForca);
            return steer;
        }

        public Vector2 wander() {
            float circleRadius = 40;
            float circleDist = 80;
            float change = 0.3f;
            
            wanderAngle += MathUtils.random(-change, change);
            
            Vector2 circleCenter = new Vector2(velocidade).nor().scl(circleDist).add(posicao);
            Vector2 displacement = new Vector2(MathUtils.cos(wanderAngle), MathUtils.sin(wanderAngle)).scl(circleRadius);
            Vector2 wanderForce = new Vector2(circleCenter).add(displacement);
            
            return seek(wanderForce);
        }

        public Vector2 separar(Array<Agente> agentes) {
            float desiredSeparation = 30f;
            Vector2 steer = new Vector2();
            int count = 0;
            for (int i = 0; i < agentes.size; i++) {
                Agente other = agentes.get(i);
                float d = posicao.dst(other.posicao);
                if ((d > 0) && (d < desiredSeparation)) {
                    Vector2 diff = new Vector2(posicao).sub(other.posicao);
                    diff.nor().scl(1 / d);
                    steer.add(diff);
                    count++;
                }
            }
            if (count > 0) {
                steer.scl(1f / count);
            }
            if (steer.len() > 0) {
                steer.setLength(maxVelocidade);
                steer.sub(velocidade);
                steer.limit(maxForca);
            }
            return steer;
        }

        public Vector2 alinhar(Array<Agente> agentes) {
            float neighbordist = 60;
            Vector2 sum = new Vector2();
            int count = 0;
            for (int i = 0; i < agentes.size; i++) {
                Agente other = agentes.get(i);
                float d = posicao.dst(other.posicao);
                if ((d > 0) && (d < neighbordist)) {
                    sum.add(other.velocidade);
                    count++;
                }
            }
            if (count > 0) {
                sum.scl(1f / count);
                sum.setLength(maxVelocidade);
                Vector2 steer = new Vector2(sum).sub(velocidade);
                steer.limit(maxForca);
                return steer;
            }
            return new Vector2();
        }

        public Vector2 coesao(Array<Agente> agentes) {
            float neighbordist = 60;
            Vector2 sum = new Vector2();
            int count = 0;
            for (int i = 0; i < agentes.size; i++) {
                Agente other = agentes.get(i);
                float d = posicao.dst(other.posicao);
                if ((d > 0) && (d < neighbordist)) {
                    sum.add(other.posicao);
                    count++;
                }
            }
            if (count > 0) {
                sum.scl(1f / count);
                return seek(sum);
            }
            return new Vector2();
        }
    }

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        npc = new Agente(400, 300);
        mousePos = new Vector2(400, 300);

        bando = new Array<>();
        for (int i = 0; i < 30; i++) {
            bando.add(new Agente(MathUtils.random(100, 700), MathUtils.random(100, 500)));
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (Gdx.input.isTouched()) {
            mousePos.set(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
        }

        float distanciaMouse = npc.posicao.dst(mousePos);
        if (distanciaMouse < 150) {
            npc.estado = Estado.PURSUIT;
        } else {
            npc.estado = Estado.WANDER;
        }

        if (npc.estado == Estado.WANDER) {
            npc.applyForce(npc.wander());
        } else {
            npc.applyForce(npc.arrive(mousePos));
        }
        npc.update();

        for (Agente agente : bando) {
            Vector2 fSep = agente.separar(bando).scl(1.5f);
            Vector2 fAli = agente.alinhar(bando).scl(1.0f);
            Vector2 fCoh = agente.coesao(bando).scl(1.0f);

            agente.applyForce(fSep);
            agente.applyForce(fAli);
            agente.applyForce(fCoh);
            agente.update();
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        shapeRenderer.setColor(Color.CYAN);
        for (Agente a : bando) {
            shapeRenderer.circle(a.posicao.x, a.posicao.y, a.raio);
        }

        if (npc.estado == Estado.WANDER) {
            shapeRenderer.setColor(Color.GREEN);
        } else {
            shapeRenderer.setColor(Color.RED);
        }
        shapeRenderer.circle(npc.posicao.x, npc.posicao.y, npc.raio + 3);

        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}